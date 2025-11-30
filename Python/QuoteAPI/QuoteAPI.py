import requests
import json
import gzip

class SimpleQuoteAPI:
    def __init__(self):
        self.base_url = "http://39.107.99.235:1008/getQuote.php"

    def get_quote(self, code):
        """获取实时行情数据"""
        if not code or not code.strip():
            raise ValueError("产品代码不能为空")

        url = f"{self.base_url}?code={code}"

        response = requests.get(url, headers={'Accept-Encoding': 'gzip'}, timeout=30)
        response.raise_for_status()

        # 处理gzip压缩
        if response.headers.get('Content-Encoding') == 'gzip':
            content = gzip.decompress(response.content).decode('utf-8')
        else:
            content = response.text

        data = json.loads(content)

        if data.get('code') != 200:
            raise Exception(f"API错误: {data.get('msg', '未知错误')}")

        return data

    def display_quote(self, quote):
        """显示行情数据"""
        body = quote['data']['body']

        print("产品代码:", body.get('StockCode'))
        print("最新价:", body.get('Price'))
        print("涨跌幅:", body.get('DiffRate'), "%")
        print("买一价:", body.get('BP1'), "买一量:", body.get('BV1'))
        print("卖一价:", body.get('SP1'), "卖一量:", body.get('SV1'))
        print("更新时间:", body.get('Time'))


# 使用示例
if __name__ == "__main__":
    api = SimpleQuoteAPI()

    try:
        quote = api.get_quote("btcusdt")
        api.display_quote(quote)
    except Exception as e:
        print("错误:", e)