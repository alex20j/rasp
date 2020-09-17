package ru.example;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import cucumber.api.java.ru.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.*;


public class Steps {

    public static String placeFrom;
    public static String placeTo;
    public static LocalTime timeMin;
    public static int priceMax;

    public static SelenideElement item;
    public static String[] itemInfo;
    public static String[] itemInnerInfo;
    public static float USD;


    @Когда("открываем страницу yandex.ru")
    public void openYandexPage() {
        open("http://yandex.ru");
    }

    @Тогда("проверяем актуальный курс доллара")
    public void checkCurrency() {
        SelenideElement stockUSD = $(byXpath("//div[a[contains(text(), 'USD')]]")).find(".inline-stocks__value_inner");
        USD = Float.parseFloat(stockUSD.getText().replaceAll("[,]", "."));
    }

    @Когда("переходим на страницу сервиса Расписания")
    public void openRaspPage() {
        $(byText("ещё")).click();
        $(byXpath("//a[@data-id='rasp']")).click();
        switchTo().window(1);
        $("h1").shouldHave(text("Расписание пригородного и междугородного транспорта"));
    }

    @Тогда("ищем электрички из \"([^\"]+)\" в \"([^\"]+)\" на ближайший/ую \"([^\"]+)\"")
    public void searchByPlaceAndDay(String from, String to, String day) {
        placeFrom = from;
        placeTo = to;
        $(byText("Электричка")).click();
        $("#from").setValue(placeFrom);
        $("#to").setValue(placeTo);
        $("#when").click();
        $$(".Calendar li").findBy(text(day)).click();
        $(byText("Найти")).click();
        sleep(2000);
    }

    @Тогда("проверяем, что название таблицы результатов соответствует параметрам поиска")
    public void checkSearch() {
        $("h1").shouldHave(matchText(".*" + placeFrom + ".*" + placeTo));
    }

    @Когда("ищем самый ранний рейс по условиям: не ранее (\\d+:\\d+), не дороже (\\d+) рублей")
    public void searchByTimeAndPrice(String time, int price) {
        timeMin = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        priceMax = price;
        SelenideElement timetable = $(".SearchSegments");
        item = FindItemByTimeAndPrice(timetable);
        itemInfo = saveItemInfo(item);
        sleep(2000);
    }

    @Тогда("открываем страницу информации о рейсе")
    public void openDetailsPage(){
        item.find(".SegmentTitle__title").click();
        sleep(2000);
    }

    @Тогда("проверяем соответствие данных на этой странице и в общем расписании")
    public void checkInfo(){
        $("h1").shouldHave(matchText(".*" + placeFrom + ".*" + placeTo));
        itemInnerInfo = saveItemInnerInfo();
        assert checkInfo(itemInfo, itemInnerInfo);
    }


    public static SelenideElement FindItemByTimeAndPrice(SelenideElement timetable) {
        ElementsCollection timetableItems = timetable.findAll(".SearchSegment");
        ElementsCollection timetableTimes = timetable.findAll(".SearchSegment .Time_important .SearchSegment__time");
        LocalTime time;
        int price;

        for (int i = 0; i < timetableTimes.size(); i++) {
            item = timetableItems.get(i);
            item.scrollTo();
            time = LocalTime.parse(timetableTimes.get(i).text());
            if (isTimeOk(time)) {
                price = extractPrice(item);
                if (price != -1){
                    showItemInfo(time, price);
                    timetableItems.get(i-1).scrollTo(); // fix element hiding
                    return item;
                }
            }
        }
        System.out.println("Рейсов, соответствующих условиям поиска, не найдено.");
        closeWebDriver();
        return item;
    }

    public static boolean isTimeOk(LocalTime time){
        return time.isAfter(timeMin) || time.equals(timeMin);
    }

    public static int extractPrice(SelenideElement timetableItem){
        ElementsCollection timetableItemPrices = timetableItem.findAll(".TariffsListItem__price");
        ArrayList<Integer> prices = new ArrayList<>();
        for (SelenideElement elem : timetableItemPrices) {
            int price = Integer.parseInt(elem.getText().replaceAll("\\D*", ""));
            prices.add(price);
        }
        return isPriceOk(prices);
    }

    public static int isPriceOk(ArrayList<Integer> prices){
        for (int price : prices) {
            if (price <= priceMax) {
                return price;
            }
        }
        return -1;
    }

    public static void showItemInfo(LocalTime time, int price) {
        System.out.println("Время отправления: " + time);
        System.out.println("Стоимость билета: " + price + " ₽ / " + convertToUSD(price, USD) + " $");
    }

    public static float convertToUSD(int price, float USD){
        float priceUSD = (float) price / USD;
        return (float) Math.round(priceUSD*100)/100;
    }

    public static String[] saveItemInfo(SelenideElement item){
        String departureTime = item.findAll(".SearchSegment__time").get(0).getText();
        String departureStation = item.find(".SegmentTitle__title").getText().split("\\s—")[0];
        String arrivalTime = item.findAll(".SearchSegment__time").get(1).getText();
        String arrivalStation = item.find(".SegmentTitle__title").getText().split("—\\s")[1];
        String duration = item.find(".SearchSegment__duration").getText();
        return new String[]{departureTime, departureStation, arrivalTime, arrivalStation, duration};
    }

    public static String[] saveItemInnerInfo(){
        String departureTime = $(".ThreadTable__rowStation_isStationFrom").find(".ThreadTable__departure").scrollTo().getText();
        String departureStation = $(".ThreadTable__rowStation_isStationFrom").find(".ThreadTable__stationLink").getText();
        String arrivalTime = $(".ThreadTable__rowStation_isStationTo").find(".ThreadTable__arrival").scrollTo().getText();
        String arrivalStation = $(".ThreadTable__rowStation_isStationTo").find(".ThreadTable__stationLink").getText();
        String duration = $(".ThreadTable__rowStation_isStationTo").find(".Duration").getText();
        return new String[]{departureTime, departureStation, arrivalTime, arrivalStation, duration};
    }

    public static boolean checkInfo(String[] info1, String[] info2){
        return Arrays.equals(info1, info2);
    }
}
