# netlify/functions/error_500.py
import json

def handler(event, context):
    print("Function 'error_500' triggered!") # Netlify Function 로그에 출력될 내용

    # === 이 부분이 500 에러를 유발하는 핵심 ===
    # Flask 앱에서 'raise Exception'을 한 것과 동일한 효과를 냅니다.
    # 함수가 처리되지 않은 예외를 발생시키면 Netlify 런타임이 이를 500 에러로 처리합니다.
    raise Exception("푸른하늘3292 님, 고의적으로 500 Internal Server Error를 발생시켰습니다!")
    # ==========================================

    # 이 아래 코드는 위의 'raise Exception' 때문에 절대 실행되지 않습니다.
    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json',
        },
        'body': json.dumps({'message': '이 메시지는 절대 볼 수 없습니다. (500 에러 발생!)'})
    }
