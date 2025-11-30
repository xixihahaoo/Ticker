import java.io.*;
import java.net.*;
import java.util.zip.GZIPInputStream;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 实时数据API
 */
public class SimpleQuoteAPI {
    private static final String BASE_URL = "http://39.107.99.235:1008/getQuote.php";
    private Gson gson = new Gson();

    /**
     * 获取实时行情数据
     */
    public JsonObject getQuote(String code) throws Exception {
        if (code == null || code.trim().isEmpty()) {
            throw new Exception("产品代码不能为空");
        }

        String url = BASE_URL + "?code=" + code;
        String response = httpGet(url);

        JsonObject result = gson.fromJson(response, JsonObject.class);
        if (result.get("code").getAsInt() != 200) {
            throw new Exception("API错误: " + result.get("msg").getAsString());
        }

        return result;
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

            if (connection.getResponseCode() != 200) {
                throw new Exception("HTTP请求错误: " + connection.getResponseCode());
            }

            // 处理gzip压缩
            InputStream inputStream;
            if ("gzip".equals(connection.getContentEncoding())) {
                inputStream = new GZIPInputStream(connection.getInputStream());
            } else {
                inputStream = connection.getInputStream();
            }

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
     * 显示行情数据
     */
    public void displayQuote(JsonObject quote) {
        JsonObject body = quote.getAsJsonObject("data").getAsJsonObject("body");

        System.out.println("产品代码: " + body.get("StockCode").getAsString());
        System.out.println("最新价: " + body.get("Price").getAsDouble());
        System.out.println("涨跌幅: " + body.get("DiffRate").getAsDouble() + "%");
        System.out.println("买一价: " + body.get("BP1").getAsDouble() + " 买一量: " + body.get("BV1").getAsDouble());
        System.out.println("卖一价: " + body.get("SP1").getAsDouble() + " 卖一量: " + body.get("SV1").getAsDouble());
        System.out.println("更新时间: " + body.get("Time").getAsString());
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        SimpleQuoteAPI api = new SimpleQuoteAPI();

        try {
            JsonObject quote = api.getQuote("btcusdt");
            api.displayQuote(quote);
        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
        }
    }
}