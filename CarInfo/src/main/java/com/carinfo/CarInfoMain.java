package com.carinfo;

import com.carinfo.service.impl.CarInfoServiceImpl;
import com.service.CarInfoService;

public class CarInfoMain {

    public static void main(String[] args) {
        CarInfoService carInfoService = new CarInfoServiceImpl();
        carInfoService.createCarInfoOutput();

    }
}
