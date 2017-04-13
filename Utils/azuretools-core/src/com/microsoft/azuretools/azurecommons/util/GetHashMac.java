/**
 * Copyright (c) Microsoft Corporation
 * <p>
 * All rights reserved.
 * <p>
 * MIT License
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.azuretools.azurecommons.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetHashMac {

    public static boolean IsValidHashMacFormat(String hashMac) {
        if (hashMac == null || hashMac.isEmpty()) {
            return false;
        }

        Pattern hashmac_pattern = Pattern.compile("[0-9a-f]{64}");
        Matcher matcher = hashmac_pattern.matcher(hashMac);
        return matcher.matches();
    }

    public static String GetHashMac() {
        String ret = null;
        String mac_raw = GetRawMac();
        if (mac_raw != null && !mac_raw.isEmpty()) {
            String mac_regex = "([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}";
            String mac_regex_zero = "([0]{2}[:-]){5}[0]{2}";
            Pattern pattern = Pattern.compile(mac_regex);
            Pattern pattern_zero = Pattern.compile(mac_regex_zero);
            Matcher matcher = pattern.matcher(mac_raw);
            String mac = "";
            while (matcher.find()) {
                mac = matcher.group(0);
                if (!pattern_zero.matcher(mac).matches()) {
                    break;
                }
            }

            ret = Hash(mac);
        }

        return ret;
    }

    private static String GetRawMac() {
        String ret = null;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] command = {"ifconfig", "-a"};
            if (os != null && !os.isEmpty() && os.startsWith("win")) {
                command = new String[]{"getmac"};
            }
            
            ProcessBuilder probuilder = new ProcessBuilder(command);
            Process process = probuilder.start();
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String tmp;
            while ((tmp = br.readLine()) != null) {
                ret += tmp;
            }

            if (inputStream != null) {
                inputStream.close();
            }
            if (br != null) {
                br.close();
            }
        } catch (IOException ex) {
            return null;
        }

        return ret;
    }

    private static String Hash(String mac) {
        if (mac == null || mac.isEmpty()) {
            return null;
        }

        String ret = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = mac.getBytes("UTF-8");
            md.update(bytes);
            byte[] bytesAfterDigest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bytesAfterDigest.length; i++) {
                sb.append(Integer.toString((bytesAfterDigest[i] & 0xff) + 0x100, 16).substring(1));
            }

            ret = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }

        return ret;
    }
}
