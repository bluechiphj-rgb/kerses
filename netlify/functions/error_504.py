# netlify/functions/error_504.py
import json
import time # 'time' 모듈은 파이썬 내장이라 requirements.txt에 추가할 필요 없음

def handler(event, context):
    print("Function 'error_504' triggered!") # Netlify Function 로그에 출력될 내용

    # === 이 부분이 504 타임아웃 에러를 유발하는 핵심 ===
    # Netlify Functions의 기본 타임아웃은 10초입니다. (유료 플랜에서는 늘릴 수 있음)
    # 여기서는 15초를 대기하여 타임아웃을 강제로 발생시킵니다.
    time.sleep(15) # 15초 대기
    # ==========================================
    
    # 위의 time.sleep(15) 때문에 10초 후에 타임아웃이 발생하여,
    # 이 return 문은 실제로는 Netlify Function 런타임에서 호출되지 않습니다.
    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json',
        },
        'body': json.dumps({'message': '이 메시지는 Netlify 타임아웃 때문에 볼 수 없습니다.'})
    }
