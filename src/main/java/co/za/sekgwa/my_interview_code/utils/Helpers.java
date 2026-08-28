package co.za.sekgwa.my_interview_code.utils;

import java.util.concurrent.ThreadLocalRandom;

public class Helpers {

    public String randomNumberGen() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return String.format("%05d", randomNumber);
    }
}
