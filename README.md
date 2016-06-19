ItemDB - [Minecraft Mod][homepage]
===============================
Version: 1.9.4-1

![IMAGE](url "Description")


0. Mod 機能一覧
---------------

  - 各機能の詳細に関しては [Wiki](../../wiki/Function) を参照してください


  - アイテムをデータベースで管理する
    - 最大で 1844京6744兆737億955万1615種類 のアイテムを保存可能
    - アイテム一種類につき 1844京6744兆737億955万1615個 まで保存可能


  - データベースを利用した特殊機能
    - 持ちきれないアイテムを自動でデータベースに収納
    - アイテムを使い切った際にデータベースから自動で補充
    - クラフト時に材料のアイテムをデータベースから自動で補充


1. 既知の不具合
---------------

  - [Issues](../../issues)を参照してください


2. 今後の更新予定
-----------------

  - GUIの改善
    - 複数のページに対応
    - 特殊機能ボタンの追加


  - 特殊機能の追加
    - プレイヤー死亡時にインベントリのアイテムをデータベースに収納
    - 周囲のインベントリからデータベースへのアイテム収納
    - データベースから周囲のインベントリへのアイテム収納
    - ユーザー指定のインベントリを監視し､収納されているアイテムを補充する


  - 内部処理
    - データベースへのアクセスを最適化
    - データベースのメンテナンス機能追加


3. インストール
---------------

  - 事前に [Minecraft Forge][forge] をインストールしておいてください
  - [DependencyResolver][resolver] をインストールしてください
  - [ダウンロード][homepage]した jar ファイルを mods フォルダに移動させます
  - 作業は以上です


4. コンフィグ
-------------

  - 設定項目の詳細に関しては [Wiki](../../wiki/Configuration) を参照してください


  - ゲーム内で変更する場合 ( 推奨 )
    - タイトル画面から Mods -> 一覧から ItemDB を選択 -> Config ボタンを押下
    - ゲームメニュー ( ゲーム中 ESC ) の Mod Options... を選択 -> 一覧から ItemDB を選択 -> Config  ボタンを押下


  - コンフィグファイルを直接編集する場合
    - Forge 環境コンフィグフォルダの moj_itemdb.cfg をエディタで編集


5. 依存関係
-----------

    Note:  
      表記されている Minecraft Forge のバージョンは開発に使用したものです  
      このバージョンでなければ動作しない､ということはないのであくまで参考程度に考えてください

  - 1.9.4-1
    - [Minecraft Forge][forge]:                         1.9.4-12.17.0.1940
    - [DependencyResolver][resolver]:                   1.0
    - [MC-Commons][commons]:                            1.9.4-0.0.1
    - [H2 Database Engine](http://www.h2database.com/): 1.4.191
    - [Slick](http://slick.lightbend.com/):             2.1.0
    - [SLF4J](http://www.slf4j.org/):                   1.6.4


6. 更新履歴
-----------

- 1.9.4-1
  - 公開


7. ライセンス
-------------

(c) Man of J, 2016

この Mod は [Minecraft Mod Public License - Version 1.0.1](./LICENSE.md) のもとで提供されています


********************************

ご意見,ご要望,バグ報告などありましたら [Issues](../../issues) か下記の連絡手段でお願いします
  - E-mail: <man.of.j@outlook.com>
  - Twitter: [_ManOfJ](https://twitter.com/_ManOfJ)

********************************

[//]: # ( リンクのエイリアス一覧 )

[homepage]: http://manofj.com/minecraft/
[forge]:    http://files.minecraftforge.net/
[resolver]: https://github.com/ManOfJ/DependencyResolver
[commons]:  https://github.com/ManOfJ/MC-Commons
