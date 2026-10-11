#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-2.0-or-later
"""Check user-visible local-preedit instructions in Java source and packaged DEX.

This is a release guard for the reviewed Japanese help, not Android UI execution.
"""
import json
import re


SECTIONS = (
    ("input help explains local drafts explicit confirmation and cancellation", "入力: 中央をタップ", (
        "通常のかな・英字・記号はキーボード内の未確定欄で編集し、確定するまで入力欄へ送りません。",
        "候補をタップ、または「確定」で送ります。",
        "「変換」はキーボード内だけで候補を仮表示します。",
        "次の文字を入力すると仮選択を取り消し、元の読みに追加します。",
        "ABC・かな・記号の切替は仮選択を取り消して読みを保持します。",
        "空白は未確定入力に追加され、確定しません。",
        "← → は入力欄のカーソルを動かして読みと仮選択を保持します。",
        "確定時の実際の選択位置へ送ります。",
        "「取消」は未確定入力をすべて破棄します。",
        "キーボードを隠す・別の入力欄へ移る・入力が再開始されると未確定入力は取り消されます。",
        "上限では自動確定せず、確定・削除・取消で続けられます。",
        "パスワード・数値・電話・日時・種類不明の欄は直接入力し、未確定欄や候補を表示しません。",
    )),
    ("learning help limits recording to successful explicit lexical choices", "初期設定はオフです。", (
        "同梱辞書で完全一致する単語を候補タップ、またはキーボード内の仮選択を「確定」で明示的に確定し、入力先への送信に成功した場合だけ、",
        "空白・続けて入力・モード切替・カーソル移動では確定せず、学習もしません。",
        "失敗した確定、分割候補は保存しません。",
    )),
    ("privacy help distinguishes local drafts from confirmed and restricted direct text", "文章の入力履歴、", (
        "パスワード欄では候補や未確定表示を行いません。",
        "未確定の読みや仮選択はキーボード内に保持し、入力先には送りません。",
        "確定した通常入力・手動変換と、パスワード・数値・電話・日時・種類不明欄の直接入力は入力先アプリに渡ります。",
        "入力先アプリや改変された OS の通信は、このキーボードの保証の範囲外です。",
    )),
    ("manual conversion help separates fixed dictionary from prediction and learning", "手動辞書変換: ", (
        "自動候補を要求しない通常の文章欄（候補禁止フラグ）でも、読みをキーボード内で編集できます。",
        "読みを入力して「変換」を押したときだけ、同梱の固定辞書の候補を表示します。",
        "保存済み学習の参照・保存と、自動の分割候補は行いません。",
        "読みを変更すると候補を閉じ、再び「変換」を押すまで表示しません。",
        "メール・URL・英字優先（FORCE_ASCII）の欄は ABC から始まります。",
        "日本語は「かな」→読み→「変換」で手動変換します。",
        "入力先が日本語の送信を拒否する場合は、読みを保持して編集・取消できます。",
        "パスワード・数値・電話・日時・種類不明の欄では手動変換もしません。",
    )),
)
RETIRED_CLAIMS = (
    "仮選択後の確定・空白",
    "続けて入力した自動確定",
    "候補が入力欄に仮表示",
    "確定、次のかな、ABC、← →",
    "空白は確定後にスペース",
    "確定した文字と通常欄の変換中の文字が渡ります",
    "入力欄が仮選択の解除を受け付けません",
    "この入力欄では候補を使いません",
    "パスワード・数値・候補禁止の欄は直接入力",
    "パスワード・数値・候補禁止欄の直接入力",
)


def java_text_values(source):
    """Decode constant text(...) literals, joining Java string concatenations."""
    literal = r'"(?:\\.|[^"\\])*"'
    bodies = re.findall(r'\btext\(\s*((?:' + literal + r'\s*\+\s*)*' + literal + r')\s*,\s*\d+\s*\)', source)
    return ["".join(json.loads(value) for value in re.findall(literal, body)) for body in bodies]


def help_checks(values, all_text=None):
    for name, prefix, required in SECTIONS:
        matches = [value for value in values if value.startswith(prefix)]
        yield name, len(matches) == 1 and all(part in matches[0] for part in required)
    text = "\n".join(values) if all_text is None else all_text
    yield "no retired inline-preview or implicit-confirmation help claims", not any(part in text for part in RETIRED_CLAIMS)
