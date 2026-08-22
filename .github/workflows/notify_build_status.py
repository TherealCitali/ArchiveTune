import os
import re
import requests


def escape_markdown_v2(text):
    escape_chars = r'_*[]()~`>#+-=|{}.!'
    return re.sub(r'([%s])' % re.escape(escape_chars), r'\\\1', text)


def main():
    bot_token = os.environ['BOT_TOKEN']
    chat_id = os.environ['CHAT_ID']
    topic_id = os.environ.get('TOPIC_ID') or os.environ.get('LOGS_TOPIC_ID')
    conclusion = os.environ.get('BUILD_CONCLUSION', 'unknown')
    run_url = os.environ.get('RUN_URL', '')
    run_id = os.environ.get('RUN_ID', '')
    sha = os.environ.get('COMMIT_SHA', '')
    short_sha = sha[:7] if sha else ''
    actor = os.environ.get('ACTOR', '')
    branch = os.environ.get('BRANCH', '')
    jobs = os.environ.get('JOB_SUMMARY', '')

    status_label = {
        'success': 'succeeded',
        'failure': 'failed',
        'cancelled': 'was cancelled',
        'skipped': 'was skipped',
    }.get(conclusion, conclusion)

    lines = [
        f"Build APKs {status_label} on `{escape_markdown_v2(branch)}` by {escape_markdown_v2(actor)}",
        f"Commit: `{escape_markdown_v2(short_sha)}`",
    ]
    if run_url:
        lines.append(f"[Open GitHub Actions log]({run_url})")
    elif run_id:
        lines.append(f"Run id: `{escape_markdown_v2(run_id)}`")
    if jobs:
        lines.append('')
        lines.append(escape_markdown_v2(jobs))

    payload = {
        'chat_id': chat_id,
        'text': '\n'.join(lines),
        'parse_mode': 'MarkdownV2',
        'disable_web_page_preview': True,
    }
    if topic_id:
        payload['message_thread_id'] = int(topic_id)

    url = f'https://api.telegram.org/bot{bot_token}/sendMessage'
    response = requests.post(url, json=payload, timeout=30)
    if response.status_code != 200:
        print(f'Failed to send status: {response.status_code} {response.text}')
    else:
        print('Build status sent.')

    log_path = os.environ.get('LOG_FILE')
    if log_path and os.path.isfile(log_path):
        with open(log_path, 'rb') as handle:
            data = {'chat_id': chat_id, 'caption': f'Actions log {short_sha}'}
            if topic_id:
                data['message_thread_id'] = int(topic_id)
            doc_url = f'https://api.telegram.org/bot{bot_token}/sendDocument'
            doc = requests.post(
                doc_url,
                data=data,
                files={'document': handle},
                timeout=120,
            )
        if doc.status_code != 200:
            print(f'Failed to send log file: {doc.status_code} {doc.text}')
        else:
            print('Log file sent.')


if __name__ == '__main__':
    main()
