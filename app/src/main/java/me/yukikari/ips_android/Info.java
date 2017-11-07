package me.yukikari.ips_android;

class Info {
    static String ipAddr = "http://192.168.1.105:8080";

    static int getResId(String desc) {
        switch (desc) {
            case "惑星メイカー":
                return R.drawable.planet_maker;
            case "ゼロトレーナー":
                return R.drawable.zero_trainer;
            case "フクオカフィールド":
                return R.drawable.fukuoka_field;
            case "エナジーシュート":
                return R.drawable.energy_shoot;
            default:
                return R.color.colorPrimary;
        }
    }
}
