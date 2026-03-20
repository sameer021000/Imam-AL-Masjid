package com.example.imam_al_masjid;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * High-precision astronomical engine for calculating Islamic prayer times with second-level accuracy.
 * Uses the NOAA Solar Calculator algorithms based on "Astronomical Algorithms" by Jean Meeus.
 */
public class CelestialWaqtEngine {

    public static class PrecisePrayerTimes {
        public Date fajr;
        public Date sunrise;
        public Date dhuhr;
        public Date asr;
        public Date maghrib;
        public Date isha;

        public PrecisePrayerTimes() {}
    }

    public static PrecisePrayerTimes calculate(double lat, double lon, Date date, double fajrAngle, double ishaAngle) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        double jd = getJulianDay(year, month, day);
        double t = (jd - 2451545.0) / 36525.0;

        double L0 = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360;
        double M = (357.52911 + t * (35999.05029 - 0.0001537 * t)) % 360;
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
        
        double C = (1.914602 - t * (0.004817 + 0.000014 * t)) * Math.sin(Math.toRadians(M))
                + (0.019993 - 0.000101 * t) * Math.sin(Math.toRadians(2 * M))
                + 0.000289 * Math.sin(Math.toRadians(3 * M));
        
        double lonSun = L0 + C;
        
        double omega = 125.04 - 1934.136 * t;
        double lambda = lonSun - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));
        double epsilon0 = 23.4392911 - t * (46.8150 / 3600 + t * (0.00059 / 3600 - t * 0.001813 / 3600));
        double epsilon = epsilon0 + 0.00256 * Math.cos(Math.toRadians(omega));
        
        double delta = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda))));
        
        double varY = Math.tan(Math.toRadians(epsilon / 2)) * Math.tan(Math.toRadians(epsilon / 2));
        double eqTime = 4 * Math.toDegrees(varY * Math.sin(Math.toRadians(2 * L0)) 
                - 2 * e * Math.sin(Math.toRadians(M)) 
                + 4 * e * varY * Math.sin(Math.toRadians(M)) * Math.cos(Math.toRadians(2 * L0))
                - 0.5 * varY * varY * Math.sin(Math.toRadians(4 * L0))
                - 1.25 * e * e * Math.sin(Math.toRadians(2 * M)));

        double solarNoon = (720 - 4 * lon - eqTime) / 60.0;
        PrecisePrayerTimes pt = new PrecisePrayerTimes();
        double h_horizon = -0.833; 
        
        pt.dhuhr = getDateFromUtcHours(date, solarNoon);
        pt.sunrise = getDateFromUtcHours(date, solarNoon - getHourAngle(lat, delta, h_horizon) / 15.0);
        pt.maghrib = getDateFromUtcHours(date, solarNoon + getHourAngle(lat, delta, h_horizon) / 15.0);
        pt.fajr = getDateFromUtcHours(date, solarNoon - getHourAngle(lat, delta, -fajrAngle) / 15.0);
        pt.isha = getDateFromUtcHours(date, solarNoon + getHourAngle(lat, delta, -ishaAngle) / 15.0);
        double asrAltitude = Math.toDegrees(Math.atan(1.0 / (1.0 + Math.tan(Math.toRadians(Math.abs(lat - delta))))));
        pt.asr = getDateFromUtcHours(date, solarNoon + getHourAngle(lat, delta, asrAltitude) / 15.0);

        return pt;
    }

    private static double getHourAngle(double lat, double delta, double h) {
        double phi = Math.toRadians(lat);
        double d = Math.toRadians(delta);
        double H_rad = Math.acos((Math.sin(Math.toRadians(h)) - Math.sin(phi) * Math.sin(d)) / (Math.cos(phi) * Math.cos(d)));
        return Math.toDegrees(H_rad);
    }

    private static double getJulianDay(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private static Date getDateFromUtcHours(Date baseDate, double utcHours) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(baseDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        long baseMs = cal.getTimeInMillis();
        long offsetMs = (long) (utcHours * 3600000);
        
        return new Date(baseMs + offsetMs);
    }
}
