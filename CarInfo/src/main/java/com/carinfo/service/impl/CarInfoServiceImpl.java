package com.carinfo.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.dto.CarInfoDto;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;
import com.service.CarInfoService;

import io.github.bonigarcia.wdm.WebDriverManager;

public class CarInfoServiceImpl implements CarInfoService {
	private static final String UK_NUMBER_PLATE_PTRN = "\\b([A-Z]{3}\\s?(\\d{3}|\\d{2}|d{1})\\s?[A-Z])|([A-Z]\\s?(\\d{3}|\\d{2}|\\d{1})\\s?[A-Z]{3})|(([A-HK-PRSVWY][A-HJ-PR-Y])\\s?([0][2-9]|[1-9][0-9])\\s?[A-HJ-PR-Z]{3})\\b";

	@Override
	public void createCarInfoOutput() {
		List<String> carRegistrationNumbers = readCarRegistrationsFromTextFile();

	}

	private List<String> readCarRegistrationsFromTextFile() {
		List<String> registrationNumbers = new ArrayList<String>();

		try {
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("car_input.txt").getFile());
			String content = FileUtils.readFileToString(file, "UTF-8");
			registrationNumbers = readTheCarNumbersFromString(content);
			List<CarInfoDto> carInfoDtoList = readCarInfo(registrationNumbers);
			List<CarInfoDto> outputCarDtoList = readOutputCarInfo();
			matchCarInfo(carInfoDtoList, outputCarDtoList);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return registrationNumbers;

	}

	private void matchCarInfo(List<com.dto.CarInfoDto> carInfoDtoList, List<com.dto.CarInfoDto> outputCarDtoList) {
		for (CarInfoDto input : carInfoDtoList) {
			for (CarInfoDto output : outputCarDtoList) {
				if (input.getRegistration().substring(0, 4)
						.equalsIgnoreCase(output.getRegistration().substring(0, 4))) {
					if (!input.getColor().equals(output.getColor())
							|| !input.getMake().equalsIgnoreCase(output.getMake())
							|| !input.getModel().equalsIgnoreCase(output.getModel())
							|| !input.getYear().equalsIgnoreCase(output.getYear())) {
						System.out.println(input.getRegistration() + "  - Vehicle Not Found,Registration incorrect");
					}
					break;
				}
			}
		}
	}

	private List<com.dto.CarInfoDto> readOutputCarInfo() {
		HeaderColumnNameTranslateMappingStrategy<CarInfoDto> beanStrategy = new HeaderColumnNameTranslateMappingStrategy<CarInfoDto>();
		beanStrategy.setType(CarInfoDto.class);

		Map<String, String> columnMapping = new HashMap<String, String>();
		columnMapping.put("REGISTRATION", "registration");
		columnMapping.put("MAKE", "make");
		columnMapping.put("MODEL", "model");
		columnMapping.put("COLOR", "color");
		columnMapping.put("YEAR", "year");

		beanStrategy.setColumnMapping(columnMapping);

		CsvToBean<CarInfoDto> csvToBean = new CsvToBean<CarInfoDto>();
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(getClass().getClassLoader().getResource("car_output.txt").getPath()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		List<CarInfoDto> carInfoDtoList = csvToBean.parse(beanStrategy, reader);
		return carInfoDtoList;
	}

	private List<CarInfoDto> readCarInfo(List<String> registrationNumbers) {
		List<CarInfoDto> carInfoDtoList = new ArrayList<>();
		WebDriverManager.chromedriver().setup();
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--headless");
		WebDriver driver = new ChromeDriver(chromeOptions);

		for (String str : registrationNumbers) {
			CarInfoDto dto = new CarInfoDto();
			driver.get("https://cartaxcheck.co.uk/");
			driver.findElement(By.cssSelector("#vrm-input")).clear();
			driver.findElement(By.cssSelector("#vrm-input")).sendKeys(str);
			driver.findElement(By.cssSelector(".jsx-3655351943")).click();
			;
			List<WebElement> datFields = driver.findElements(By.cssSelector(".jsx-3496807389"));
			int count = 0;
			for (WebElement data : datFields) {
				dto.setRegistration(str);
				if (count == 0) {
					if (data.getText() != null) {
						dto.setRegistration(data.getText());
					}
				} else if (count == 1) {
					dto.setMake(data.getText());
				} else if (count == 2) {
					dto.setModel(data.getText());
				} else if (count == 3) {
					dto.setColor(data.getText());
				} else if (count == 4) {
					dto.setYear(data.getText());
				} else {
					break;
				}

				count++;
			}
			carInfoDtoList.add(dto);

		}
		driver.quit();
		return carInfoDtoList;
	}

	private List<String> readTheCarNumbersFromString(String content) {
		List<String> numbers = new ArrayList<>();
		Pattern ptr = Pattern.compile(UK_NUMBER_PLATE_PTRN);
		Matcher matcher = ptr.matcher(content);
		while (matcher.find()) {
			numbers.add(matcher.group());
		}
		return numbers;
	}

}
