package com.campusnavi.backend.infra.redis;

public final class RedisKeys {

    public static String refreshToken(String jti){
        return "auth:refresh:" + jti;
    }

    public static String blacklist(String jti){
        return "auth:blacklist:" + jti;
    }

    public static String emailCode(String email){
        return "auth:email:code:" + email;
    }

    public static String emailVerified(String verifiedToken){
        return "auth:email:verified:" + verifiedToken;
    }

    public static String emailCooldown(String email){
        return "auth:email:cooldown:" + email;
    }

    public static String emailRequestIp(String ip){
        return "auth:email:request:" + ip;
    }

    public static String emailBlockIp(String ip){
        return "auth:email:block:" + ip;
    }

    public static String emailVerifyFail(String email) {
        return "auth:email:verify:fail:" + email;
    }

    public static String emailVerifyBlock(String email) {
        return "auth:email:verify:block:" + email;
    }

}
