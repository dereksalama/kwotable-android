package com.dereksalama.kwotabl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dereksalama on 9/4/14.
 */
public class ChecksumUtil {

    private ChecksumUtil () {}

    private static final String SECRET = "donttellanyone";

    public static String makeCheck(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        BigInteger bn = new BigInteger(1,md.digest(SECRET.getBytes()));
        return bn.toString(16);
    }
}
