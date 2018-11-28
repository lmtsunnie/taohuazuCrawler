package com.sunnie.taohuazu.pic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Crawler {
    private final static Logger logger = LogManager.getLogger(Crawler.class);

    private static List<String> userAgents;

    static {
        // 相当于返回一个new ArrayList，链式编程
        userAgents = Lists.newArrayList();
        // 构造五个User-Agent
        userAgents.add("spider");
        userAgents.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1; rv:63.0) Gecko/20100101 Firefox/63.0");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
        userAgents.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:63.0) Gecko/20100101 Firefox/63.0");
    }

    public static Map<String, String> headers() {
        // Maps.newHashMap是static方法，返回一个new HashMap，只是为了方便链式编程
        Map<String, String> headers = Maps.newHashMap();
        // 在userAgents里面随机选取一个构成<"User-Agent","xxx">键值对
        headers.put("User-Agent", userAgents.get(Math.abs(new Random().nextInt()) % userAgents.size()));
        return headers;
    }

    public static String httpGet(String url) {
        // 得到一个用默认参数构造的CloseableHttpClient并将其传入HttpClient
        // CloseableHttpClient实现了HttpClient,Closeable两个接口
        org.apache.http.client.HttpClient httpClient = HttpClients.createDefault();
        try {
            // HttpGet是以实体的形式返回Request-URI标识的信息
            HttpGet httpGet = new HttpGet(url);
            // httpGet.setHeader(k,v)
            headers().forEach(httpGet::setHeader);
            // 模拟一次http请求返回httpResponse
            HttpResponse httpResponse = httpClient.execute(httpGet);
            // httpResponse转成UTF-8编码的字符串
            return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public static InputStream getPicture(String url) {
        org.apache.http.client.HttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet httpGet = new HttpGet(url);
            headers().forEach(httpGet::setHeader);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            return httpResponse.getEntity().getContent();
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public static void savePic(String url) {
        System.out.println("picture url " + url);
        String dir = "/Users/sunnie/sunniedoc/taohuazu/";
        String filename = String.valueOf(System.currentTimeMillis());
        InputStream inputStream = getPicture(url);
        File file = new File(dir);
        if (!file.exists()) {
            if (!file.mkdirs()) {//若创建文件夹不成功
                System.out.println("Unable to create external cache directory");
            }
        }

        File targetfile = new File(dir + filename);
        OutputStream os = null;
        try {
            os = new FileOutputStream(targetfile);
            int ch = 0;
            while ((ch = inputStream.read()) != -1) {
                os.write(ch);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> getRegExContent(String content, String regex) {
        List<String> list = new ArrayList<>();
        // Pattern Matcher
        Pattern pa = Pattern.compile(regex, Pattern.CANON_EQ);
        Matcher ma = pa.matcher(content);
        while (ma.find()) {
            list.add(ma.group(1));
        }
        return list;
    }

    public static void singlePageUrlList(String url) {
        System.out.println("website url " + url);
        String content = httpGet(url);
        List<String> urlList = getRegExContent(content, "class=\"zoom\" src=\"(.*?)\"");
        if (urlList.isEmpty()) {
            urlList = getRegExContent(content, "class=\"zoom\" file=\"(.*?)\"");
        }
        // 把每个url都作为图片存下来
        urlList.forEach(Crawler::savePic);
    }

    public static void main(String[] args) {
        String urlPrefix = "http://thzbt.co/";
        String urlPagePrefix = "http://thzbt.co/forum-181-";
        IntStream.range(1,10).forEach(i -> {
            String url = urlPagePrefix + i +".html";
            String content = httpGet(url);
            List<String> webUrlList = getRegExContent(content, "<a href=\"(thread-.*?.html)\"");
            webUrlList = webUrlList.stream().distinct().collect(Collectors.toList());
            webUrlList.forEach(webSite -> singlePageUrlList(urlPrefix + webSite));
        });
    }
}



