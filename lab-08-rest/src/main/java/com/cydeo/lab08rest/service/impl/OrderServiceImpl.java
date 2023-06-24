package com.cydeo.lab08rest.service.impl;

import com.cydeo.lab08rest.client.CurrencyApiClient;
import com.cydeo.lab08rest.dto.OrderDTO;
import com.cydeo.lab08rest.dto.UpdateOrderDTO;
import com.cydeo.lab08rest.entity.Order;
import com.cydeo.lab08rest.enums.Currency;
import com.cydeo.lab08rest.exception.CurrencyTypeNotFoundException;
import com.cydeo.lab08rest.exception.NotFoundException;
import com.cydeo.lab08rest.mapper.MapperUtil;
import com.cydeo.lab08rest.repository.OrderRepository;
import com.cydeo.lab08rest.service.CartService;
import com.cydeo.lab08rest.service.CustomerService;
import com.cydeo.lab08rest.service.OrderService;
import com.cydeo.lab08rest.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderServiceImpl implements OrderService {

    @Value("${access_key}")
    private String accessKey;
    private final OrderRepository orderRepository;
    private final MapperUtil mapperUtil;
    private final CustomerService customerService;
    private final PaymentService paymentService;
    private final CartService cartService;
    private final CurrencyApiClient currencyApiClient;

    public OrderServiceImpl(OrderRepository orderRepository, MapperUtil mapperUtil, CustomerService customerService,
                            PaymentService paymentService, CartService cartService, CurrencyApiClient currencyApiClient) {
        this.orderRepository = orderRepository;
        this.mapperUtil = mapperUtil;
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.cartService = cartService;
        this.currencyApiClient = currencyApiClient;
    }

    @Override
    public List<OrderDTO> retrieveOrderList() {
        //retrieve all orders and return as a List of OrderDTO
        return orderRepository.findAll().stream()
                .map(order -> mapperUtil.convert(order,new OrderDTO())).collect(Collectors.toList());
    }

    @Override
    public OrderDTO updateOrder(OrderDTO orderDTO) {
        //look for the orderId inside the database and throw the exception
        Order order = orderRepository.findById(orderDTO.getId()).orElseThrow(
                () -> new NotFoundException("Order could not be found."));

        //then we need to check if the order fields are exist or not
        validateRelatedFieldsAreExist(orderDTO);

        //if fields are exists, then convert orderDTO to order and save it
        Order willBeUpdatedOrder = mapperUtil.convert(orderDTO, new Order());
        Order updatedOrder = orderRepository.save(willBeUpdatedOrder);
        //convert again the updated one and return it
        return mapperUtil.convert(updatedOrder,new OrderDTO());
    }

    @Override
    public OrderDTO updateOrderById(Long id, UpdateOrderDTO updateOrderDTO) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Order could not be found."));
        //if we are getting the same value, it is not necessary to update the actual value

        boolean changeDetected = false;

        if (!order.getPaidPrice().equals(updateOrderDTO.getPaidPrice())){
           order.setPaidPrice(updateOrderDTO.getPaidPrice());
           changeDetected =true;
        }

        if(!order.getTotalPrice().equals(updateOrderDTO.getTotalPrice())){
            order.setTotalPrice(updateOrderDTO.getTotalPrice());
            changeDetected=true;
        }

        //if there is any change, update the order and return it
        if(changeDetected){
            Order updateOrder = orderRepository.save(order);
            return mapperUtil.convert(updateOrder,new OrderDTO());
        }else{
            throw new RuntimeException("No changes detected");
        }

    }

    @Override
    public OrderDTO retrieveOrderDetailById(Long id, Optional<String> currency) {
        //find the order based on id
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Order could not be found."));

        //if we are getting currency value from the user
        currency.ifPresent(curr -> {
            //check that if user currency input is valid(inside the our currencies list)
            validateCurrency(curr);
            //get the currency data based on currency type
            BigDecimal currencyRate = getCurrencyRate(curr);
            //do calculations and set new paidPrice and totalPrice
            //these prices for just to give value to customer, we will not update the db based on other currencies
            BigDecimal newPaidPrice = order.getPaidPrice().multiply(currencyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newTotalPrice = order.getTotalPrice().multiply(currencyRate).setScale(2, RoundingMode.HALF_UP);
            //set the value to order that we retrieved
            order.setPaidPrice(newPaidPrice);
            order.setTotalPrice(newTotalPrice);
        });

        //convert and return it
        return mapperUtil.convert(order,new OrderDTO());
    }

    private void validateCurrency(String curr) {
        //check if the currency is valid currency
        List<String> currencies = Stream.of(Currency.values())
                .map(currency -> currency.value)
                .collect(Collectors.toList());

        boolean isCurrencyValid = currencies.contains(curr);

        if (!isCurrencyValid){
            throw new CurrencyTypeNotFoundException("Currency type for "+ curr +" could not be found.");
        }
    }

    private BigDecimal getCurrencyRate(String currency) {
        //consume the api
        //we save response inside the quotes map

        Map<String, Double> quotes = (Map<String, Double>) currencyApiClient.getCurrencyRates(accessKey, currency, "USD", 1).get("quotes");
        Boolean isSuccess = (Boolean) currencyApiClient.getCurrencyRates(accessKey, currency, "USD", 1).get("success");
        //check if success if true then retrieve
        if(!isSuccess) {
            throw new RuntimeException("API IS DOWN");
        }
        String expectedCurrency = "USD"+currency.toUpperCase();
        BigDecimal currencyRate = BigDecimal.valueOf(quotes.get(expectedCurrency));
        //check if currencyRate is valid
        return currencyRate;
    }

    private void validateRelatedFieldsAreExist(OrderDTO orderDTO) {
        //in this method we have 3 different service and make sure they have those fields
        //we will create service and existById method and verify

        if(!customerService.existById(orderDTO.getCustomerId())){
            throw new NotFoundException("Customer could not be found");
        }

        if(!paymentService.existById(orderDTO.getPaymentId())){
            throw new NotFoundException("Payment could not be found");
        }

        if(!cartService.existById(orderDTO.getCartId())){
            throw new NotFoundException("Cart could not be found");
        }


    }
}
