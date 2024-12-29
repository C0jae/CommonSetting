package com.example.commonsetting.global.util;

import com.example.commonsetting.global.infra.mail.dto.MailDto;
import com.example.commonsetting.global.infra.mail.type.MailDecoration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.time.ZoneId.systemDefault;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class ComUtils {

    public static final String TRACE_ID = "traceId";
    public static final String MASK_CHAR = "*";

    private ComUtils() {
    }

    public static final String[] FILTER_IGNORE_PATH = new String[]{"/health-check"};

    public static <T> Stream<T> collectionAsStream(Collection<T> t) {
        return t.stream().flatMap(Stream::ofNullable);
    }

    public static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Integer.parseInt(strNum);
        } catch (Exception nfe) {
            return false;
        }
        return true;
    }
    public static boolean isValidPhoneNumber(String number) {
        Pattern pattern = Pattern.compile("\\d{3}-\\d{4}-\\d{4}");
        Matcher matcher = pattern.matcher(number);
        return matcher.matches();
    }
    public static String maskingTelNo(String strText, boolean lastNumber) {
        if (StringUtils.isEmpty(strText)) return "";
        int length = strText.length();
        int maskLength = lastNumber ? 1 : 2;
        String strMaskString = StringUtils.repeat(MASK_CHAR, maskLength);
        int i = strText.length() - maskLength;
        if(lastNumber) {
            return StringUtils.overlay(strText, strMaskString, 0, 1);
        }
        else return StringUtils.overlay(strText, strMaskString, i, length);
    }

    public static String maskingId(String strText) {
        if (StringUtils.isEmpty(strText)) return "";

        boolean isEmailType = isValidEmailAddress(strText);
        String maskPart;
        String emailPart = "";
        try {
            if (isEmailType) {
                String[] parts = strText.split("@");
                maskPart = parts[0];
                emailPart = "@" + parts[1];
            } else {
                maskPart = strText;
            }

            int length = maskPart.length();
            int maskLength = 2;
            String strMaskString;
            if (length <= maskLength + 1) {
                strMaskString = StringUtils.repeat(MASK_CHAR, 1);
                String overlay = StringUtils.overlay(maskPart, strMaskString, length - 1, length);
                return String.format("%s%s", overlay, emailPart);
            } else {
                strMaskString = StringUtils.repeat(MASK_CHAR, 3);
                String overlay = StringUtils.overlay(maskPart, strMaskString, length - 3, length);
                return String.format("%s%s", overlay, emailPart);
            }
            // return String.format("%s%s", maskPart.replaceAll("(?<=.{" + (maskLength+1) + "}).", maskChar), emailPart) ;
        } catch (NullPointerException e) {
            log.warn("making error => {}", ExceptionUtils.getStackTrace(e));
            return strText;
        } catch (Exception e) {
            log.warn("making error = {}", ExceptionUtils.getStackTrace(e));
            return strText;
        }
    }

    public static String maskingName(String strText) {
        if (StringUtils.isEmpty(strText)) return "";
        try {
            // 30% -> *
            int length = strText.length();
            int count = (int) Math.floor((double) length / 3);
            int maskingCount = count == 0 ? 1 : count;
            String strMaskString = StringUtils.repeat(MASK_CHAR, maskingCount);
            int i = length - maskingCount;
            return StringUtils.overlay(strText, strMaskString, i, length);
        } catch (NullPointerException e) {
            log.warn("making error => {}", ExceptionUtils.getStackTrace(e));
            return strText;
        } catch (Exception e) {
            log.warn("making error = {}", ExceptionUtils.getStackTrace(e));
            return strText;
        }
    }

    public static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        Pattern p = Pattern.compile(ePattern);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    public static boolean isValidDateFormat(String dateStr, String pattern) {
        DateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public static String resolveToken(String bearerToken) {
        if (isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public static String encodeWithSHA256(String source) {
        MessageDigest sh = null;
        try {
            if(StringUtils.isBlank(source)) return source;

            sh = MessageDigest.getInstance("SHA-256");

            sh.update(source.getBytes());
            byte[] byteData = sh.digest();
            StringBuilder sb = new StringBuilder();
            for (byte byteDatum : byteData) {
                sb.append(Integer.toString((byteDatum & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
            return source;
        }
    }

    public static String convertBytesWithUnits(Long bytes) {
        var units = new String[]{"bytes", "KB", "MB", "GB", "TB", "PB"};
        var formatSize = "0";
        if(bytes != 0) {
            var idx = (int) Math.floor(Math.log(bytes) / Math.log(1024));
            var formatter = new DecimalFormat("#,###.##");
            formatSize = formatter.format((bytes / Math.pow(1024, idx))).concat(" ").concat(units[idx]);
        } else {
            formatSize = formatSize.concat(" ").concat(units[0]);
        }
        return formatSize;
    }

    public static int convertBytes(Long bytes) {
        var formatSize = 0;
        if(bytes != 0) {
            var idx = (int) Math.floor(Math.log(bytes) / Math.log(1024));
            formatSize = (int) (bytes / Math.pow(1024, idx));
        }
        return formatSize;
    }

    public static String removeHyphen(String input) {
        return input == null ? null : input.replace("-", "");
    }

    public static MailDto getMailInfo(List<String> mailToList, Map<String, Object> variables, MailDecoration mailDecoration) {
        // html template
        var service = String.valueOf(variables.get("service"));
        var content = new MailDto.Content(mailDecoration.getTemplateHtml(), variables);
        return new MailDto(mailToList, mailDecoration.getTitleMessageCode(),service, content);
    }

    public static boolean isExpiresAt(ZonedDateTime expiresAt) {
        try {
            LocalDateTime expireDateTime = expiresAt.withZoneSameInstant(systemDefault()).toLocalDateTime();
            return LocalDateTime.now().isAfter(expireDateTime);
        } catch (Exception e) {
            return true;
        }
    }

    public static String createSalt(){
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            byte[]salt = new byte[10];
            secureRandom.nextBytes(salt);
            StringBuilder sb = new StringBuilder();
            for (byte b : salt) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            log.error("{} ", ExceptionUtils.getStackTrace(e));
        }
        return "";
    }

    public static String formatDateDefaultIfNull(LocalDateTime time, DateTimeFormatter formatter, String defaultValue) {
        return time == null ? defaultValue : time.format(formatter);
    }
}