import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * K线图数据接口调用类
 * 支持获取外汇K线数据
 */
public class KLineAPI {
    private static final String BASE_URL = "http://39.107.99.235:1008/redis.php";
    private Gson gson;

    public KLineAPI() {
        this.gson = new GsonBuilder().create();
    }

    /**
     * 获取K线数据
     *
     * @param code 产品代码
     * @param timeFrame 时间格式 (1m, 5m, 15m, 30m, 1h, 1d, 1M)
     * @param rows 获取条数
     * @return K线数据列表
     * @throws Exception
     */
    public List<List<Object>> getKLineData(String code, String timeFrame, int rows) throws Exception {
        // 参数验证
        validateParams(code, timeFrame, rows);

        // 构建请求URL
        String url = buildRequestUrl(code, timeFrame, rows);

        // 发送HTTP请求
        String response = httpGet(url);

        // 解析JSON响应
        return parseJsonResponse(response);
    }

    /**
     * 参数验证
     */
    private void validateParams(String code, String timeFrame, int rows) throws Exception {
        if (code == null || code.trim().isEmpty()) {
            throw new Exception("产品代码不能为空");
        }

        String[] validTimeframes = {"1m", "5m", "15m", "30m", "1h", "1d", "1M"};
        boolean valid = false;
        for (String tf : validTimeframes) {
            if (tf.equals(timeFrame)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new Exception("时间格式不支持，支持的格式: " + String.join(", ", validTimeframes));
        }

        if (rows <= 0) {
            throw new Exception("获取条数必须大于0");
        }
    }

    /**
     * 构建请求URL
     */
    private String buildRequestUrl(String code, String timeFrame, int rows) {
        return BASE_URL + "?code=" + code + "&time=" + timeFrame + "&rows=" + rows;
    }

    /**
     * HTTP GET请求
     */
    private String httpGet(String urlString) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("HTTP请求错误，状态码: " + responseCode);
            }

            // 处理gzip压缩响应
            String contentEncoding = connection.getContentEncoding();
            InputStream inputStream;
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                inputStream = new GZIPInputStream(connection.getInputStream());
            } else {
                inputStream = connection.getInputStream();
            }

            // 读取响应内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 解析JSON响应
     */
    private List<List<Object>> parseJsonResponse(String response) throws Exception {
        try {
            Type listType = new TypeToken<List<List<Object>>>(){}.getType();
            return gson.fromJson(response, listType);
        } catch (Exception e) {
            throw new Exception("JSON解析错误: " + e.getMessage());
        }
    }

    /**
     * 格式化K线数据（便于阅读）
     */
    public List<Map<String, Object>> formatKLineData(List<List<Object>> data) {
        List<Map<String, Object>> formatted = new ArrayList<>();

        for (List<Object> item : data) {
            Map<String, Object> formattedItem = new LinkedHashMap<>();
            formattedItem.put("timestamp", item.get(0));
            formattedItem.put("open", item.get(1));
            formattedItem.put("high", item.get(2));
            formattedItem.put("low", item.get(3));
            formattedItem.put("close", item.get(4));
            formattedItem.put("datetime", item.get(5));
            formattedItem.put("volume", item.get(6));
            formatted.add(formattedItem);
        }

        return formatted;
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        KLineAPI api = new KLineAPI();

        try {
            // 获取英镑兑美元1分钟K线数据，最新40条
            System.out.println("获取K线数据...");
            List<List<Object>> klineData = api.getKLineData("fx_sgbpusd", "1m", 40);

            // 原始数据
            System.out.println("\n原始K线数据:");
            for (int i = 0; i < klineData.size(); i++) {
                System.out.println((i + 1) + ". " + klineData.get(i));
            }

            // 格式化后的数据
            System.out.println("\n格式化后的K线数据:");
            List<Map<String, Object>> formattedData = api.formatKLineData(klineData);
            for (int i = 0; i < formattedData.size(); i++) {
                System.out.println((i + 1) + ". " + formattedData.get(i));
            }

            // 数据统计
            System.out.println("\n数据统计:");
            System.out.println("获取到 " + klineData.size() + " 条K线数据");
            if (!klineData.isEmpty()) {
                List<Object> latest = klineData.get(0);
                System.out.println("最新数据时间: " + latest.get(5));
                System.out.println("开盘: " + latest.get(1) + ", 最高: " + latest.get(2) +
                        ", 最低: " + latest.get(3) + ", 收盘: " + latest.get(4));
            }

        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
}