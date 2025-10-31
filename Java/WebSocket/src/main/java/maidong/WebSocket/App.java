package maidong.WebSocket;

/**
 * 行情对接地址：http://39.107.99.235:1008/market
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        Client client = new Client();
        client.doConnect();
    }
    
    public static void sleep(long time) {
    	try {
			Thread.sleep(time);
		} catch (Exception e) {
			// TODO: handle exception
		}
    }
}
