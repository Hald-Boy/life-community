# -*- coding: utf-8 -*-
"""
news-community-cloud 社区数据初始化生成器（独立于业务代码）

产出：
  1) seed.sql                     可直接 mysql 导入的整库刷新脚本（UTF-8）
  2) uploads/news/*.jpg           帖子配图（本地真实文件，URL 指向 http://localhost:8080/uploads/news/xxx.jpg）
  3) uploads/avatar/*.png         用户默认头像（首字 + 渐变底）
  4) uploads/cover/*.jpg          部分用户的个人主页封面

用法（在项目根目录执行）：
  python seed.py                 # 只生成 seed.sql 与图片，不动数据库
  python seed.py --apply         # 生成并导入数据库（会清空所有帖子与互动数据，users 只清 16~95）
  python seed.py --verify        # 只跑对账校验（打印不一致项）
  python seed.py --flush-redis   # 灌完数据后清空 Redis（下次启动服务由预热重建点赞/热评）

设计要点：
  * 写入顺序遵循依赖：user -> news -> news_image -> comment -> 互动流水 -> 举报/通知
  * 所有冗余计数（like_count/comment_count/collect_count/total_like_count/follow_count/fan_count）
    都由真实互动行数回算，不做独立随机，避免“点赞 300 但只有 17 条点赞记录”的假数据痕迹
  * id 按时间顺序连续分配（帖子、评论、点赞、关注等都符合自增表的真实形态）
  * 随机种子固定，重复生成结果一致
"""

import argparse
import os
import random
import subprocess
import sys
from datetime import datetime, timedelta

from PIL import Image, ImageDraw, ImageEnhance, ImageFont

# ----------------------------------------------------------------------------
# 配置
# ----------------------------------------------------------------------------
ROOT = os.path.dirname(os.path.abspath(__file__))
DB = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "name": "news_course_design",
}
MYSQL_BIN = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
REDIS_CLI = r"D:\Mysoft\IT\Redis\Redis-x64-3.2.100\redis-cli.exe"

UPLOAD_NEWS = os.path.join(ROOT, "uploads", "news")
UPLOAD_AVATAR = os.path.join(ROOT, "uploads", "avatar")
UPLOAD_COVER = os.path.join(ROOT, "uploads", "cover")
POOL_DIR = os.path.join(ROOT, "seed_assets", "pool")
SQL_PATH = os.path.join(ROOT, "seed.sql")
MANIFEST = os.path.join(ROOT, "seed_assets", "generated_images.txt")

BASE_URL = "http://localhost:8080/uploads"

# 由项目自带 org.mindrot.jbcrypt 0.4 生成并校验过的 “123456” 哈希
PASSWORD_HASH = "$2a$10$KUVGTUeXHnvfsZYIrmQQS.5rBQnkKKs3YH48ATjt.OJx7LJkXAWCK"

NOW = datetime(2026, 11, 5, 9, 30, 0)      # 数据生成的“当前时间”锚点
RNG = random.SystemRandom()               # 占位，实际使用固定种子的 random.Random
SEED = 20261105

# 已存在的历史账号（保留不动），新种子用户从 16 开始
USER_ID_START = 16
USER_COUNT = 80
POST_COUNT = 400

LEGACY_ACCOUNTS = {
    "12345678", "87654321", "23456789", "34567891", "45678912", "56789123",
    "67891234", "78912345", "89123456", "91234567", "59652367239",
    "43964822138", "75511201225", "42068299943", "40099294259",
}
LEGACY_PHONES = {
    "15177575833", "13529919830", "18201682930", "19237837944", "15678446421",
    "15935746281", "12358945624", "78542656623", "45587123655", "65842698532",
    "19173771254", "19237837999", "19237837988", "13527919830", "13800001234",
}
LEGACY_NAMES = {
    "张三", "李四", "麦晓雯", "黄中武", "吴林武", "妲己", "陈真", "庞士元",
    "方长", "测试注册用户", "用户r6dq", "庞媛媛", "星空之下", "用户eyhw", "用户85yi",
}

HOUR_WEIGHTS = [
    (0, 2), (1, 1), (2, 1), (3, 1), (4, 1), (5, 2), (6, 3), (7, 6),
    (8, 7), (9, 8), (10, 8), (11, 8), (12, 10), (13, 9), (14, 8), (15, 8),
    (16, 8), (17, 8), (18, 9), (19, 10), (20, 12), (21, 13), (22, 12), (23, 7),
]

# ----------------------------------------------------------------------------
# 语料：昵称 / 简介 / 地点
# ----------------------------------------------------------------------------
NICKNAMES = [
    "阿柚不加冰", "城南旧事", "芝士焗饭", "三分甜", "十二点半", "摸鱼第一名",
    "小熊饼干碎", "咸鱼不想翻身", "一颗小行星", "晚风骑行者", "泡面加个蛋",
    "林间有雾", "白桃乌龙", "阿彬今天不加班", "凌晨四点的猫", "一个人看海",
    "半糖少冰", "酸菜牛肉面", "数码界小学生", "键帽收藏家", "帧数警察",
    "大坝摸金校尉", "老六本人", "摸金不成反被莽", "狙击手阿豪", "宿舍第一狙",
    "显卡烧了", "装机小能手", "只买二手", "显示器调色师", "耳机听个响",
    "考研倒计时", "秋招战神", "简历改了八遍", "八股文背诵机", "图书馆钉子户",
    "早八战神", "期末周发疯", "实验报告生成器", "答辩前一晚", "高数重修选手",
    "桂航小透明", "博白阿明", "玉林老表", "桂林米粉鉴定师", "螺蛳粉狂魔",
    "食堂窗口阿姨", "三楼靠窗位", "夜跑打卡人", "宿舍断电记", "洗衣房守夜人",
    "想睡到自然醒", "秋裤战士", "校门口的猫", "便利店常客", "肥宅快乐水",
    "今天必须早睡", "实习人打工魂", "后端搬砖工", "前端不背锅", "Redis爱好者",
    "MySQL杀手", "微服务劝退师", "Docker拉黑我", "代码写完了吗", "需求又变了",
    "甲方快乐器", "三分钟热度", "会写诗的bug", "键盘侠退役", "冷静的观察者",
    "不想起名的阿哲", "小城市青年", "北纬25度", "山雾散尽", "橘子汽水",
    "十七岁的夏天", "慢半拍", "无名之辈", "叶子不黄", "梅子酒加冰",
]

BIOS = [
    "软件工程大四，正在找后端岗，会点 Java，不会吹",
    "只发吃的，不接广告",
    "游戏打得菜，嘴很硬",
    "图书馆四楼常驻，欢迎来抢座",
    "在准备考研，别喊我出去玩",
    "大三，做后端的，最近在折腾微服务",
    "记录日常，偶尔吐槽，理性发言",
    "桂林读书，玉林人，米粉鉴定师",
    "喜欢拍晚霞，手机拍的，别嫌糊",
    "数码爱好者，二手捡漏选手",
    "游戏只玩单机，不玩联机，怕被骂",
    "早八人，永远在赶路",
    "在实习，通勤两小时，命是咖啡给的",
    "不会写代码，但会改需求",
    "佛系冲浪，随缘回关",
    "打工人预备役，简历改了八遍",
    "宿舍养了盆绿萝，活得比我好",
    "夜跑三年，膝盖已报警",
    "怕冷，十月份就穿秋裤",
    "拍照只会按快门，构图靠缘分",
    "",
    "",
    "",
    "",
    "",
]

LOCATIONS = [
    "广西桂林", "广西桂林", "广西桂林", "广西桂林", "广西玉林", "广西玉林",
    "广西南宁", "广西柳州", "广西贵港", "广东深圳", "广东广州", "广东东莞",
    "湖南长沙", "贵州贵阳", "云南昆明", "海南海口", "广东佛山", "广西钦州",
    "", "",
]

# ----------------------------------------------------------------------------
# 语料：32 个场景（标题核心 + 正文句子 + 图片池）
# ----------------------------------------------------------------------------
SCENES = {
    "library_seat": {
        "theme": "campus",
        "titles": ["图书馆四楼又满了", "下午四点的图书馆", "抢座实录", "自习到闭馆", "四楼靠窗位又被占了"],
        "lines": [
            "下午两点去图书馆，四楼靠窗的位置全没了，只剩墙角一个没插座的位置",
            "占座的人一半不在，桌上堆着书和水杯，人不知道去哪了",
            "我一般坐三楼，人少，缺点是空调太冷，得带件外套",
            "今天硬坐了两个小时，效率还行，至少把昨天的实验报告写完了",
            "闭馆音乐一响，整层楼同时开始收东西，那场面挺壮观",
            "提醒一下，现在晚上十点半清场，别睡过头被保安叫醒",
        ],
        "img": ["library_01", "library_02", "library_03"],
    },
    "canteen_queue": {
        "theme": "campus",
        "titles": ["食堂排队排到楼梯口", "十二点的食堂", "二楼窗口的粉蒸肉", "食堂是不是涨价了", "打饭阿姨的手到底抖不抖"],
        "lines": [
            "中午十二点十分到食堂，队伍已经排到楼梯拐角，前面大概四十个人",
            "二楼最右边那个窗口的粉蒸肉是真的香，就是每次只剩一点点",
            "今天打了三个菜一共十四块，感觉比上学期贵了两块，不知道是不是我记错",
            "阿姨给我的肉明显比别人少，我怀疑是看我不顺眼，但我没说",
            "建议十一点半就去，人少菜全，就是要牺牲一节课的时间",
            "食堂的汤永远是温的，喝完更渴",
        ],
        "img": ["food_04", "food_01"],
    },
    "dorm_life": {
        "theme": "campus",
        "titles": ["宿舍今晚又断电了", "室友的作息我真的受不了", "宿舍楼下的雨", "洗衣房的队伍", "我们宿舍的绿萝活下来了"],
        "lines": [
            "晚上十一点准时断电，我的电脑刚跑到一半，直接黑屏，作业没保存",
            "室友每天凌晨两点还在打游戏，键盘声一直响，我说了三次，他答应得好好的",
            "昨晚下大雨，从窗户往外看，路灯把雨照得像在下雪，还挺好看",
            "洗衣房四台机器坏了两个，剩下两个永远有人排队，我已经抱着衣服站了四十分钟",
            "宿舍四个人共用一个插排，谁先插上谁有理",
            "隔壁宿舍搬走了，留下一股泡面味，三天没散",
        ],
        "img": ["campus_02", "life_03"],
    },
    "early_class": {
        "theme": "campus",
        "titles": ["早八人今天的命是闹钟给的", "七点五十的校园", "又迟到了", "秋天的早上真起不来", "早八教室的第一排"],
        "lines": [
            "闹钟从六点四十响到七点十分，我才爬起来，刷牙的时候还在想要不要装病",
            "七点五十的校园很安静，只有保洁阿姨和几个和我一样狂奔的人",
            "今天差点迟到，进门的时候老师已经在讲第二页了，我从前门走到最后一排，全程被看着",
            "秋天的早上真的起不来，被子一裹就不想动，感觉床有引力",
            "第一排永远没人坐，我坐了一次，被点名回答了三个问题",
            "早八最痛苦的不是上课，是要在前一晚十二点前睡着",
        ],
        "img": ["campus_03", "campus_01"],
    },
    "express_station": {
        "theme": "campus",
        "titles": ["快递站又排长队", "菜鸟驿站的阿姨记住了我", "取个快递像赶集", "寄快递被收了十二块", "快递柜满了"],
        "lines": [
            "下课去快递站，前面排了三十多个人，队伍拐了两个弯",
            "驿站阿姨已经认识我了，看到我就直接把我那个箱子拎出来，效率高得吓人",
            "今天寄一个两公斤的件，被收了十二块，我当场沉默",
            "快递柜全满，只能放驿站，结果驿站六点半就关门，我下课赶过去刚好关灯",
            "双十一前后建议错峰，晚上九点去人最少，就是得摸黑找箱子",
            "我的快递被别人取走了，监控看了半天，最后是隔壁宿舍拿错，箱子已经拆了",
        ],
        "img": ["campus_03", "campus_02"],
    },
    "campus_cat": {
        "theme": "campus",
        "titles": ["宿舍楼下那只狸花", "今天又喂了猫", "猫比我受欢迎", "教学楼后面的小猫", "谁把猫抱走了"],
        "lines": [
            "宿舍楼下那只狸花今天蹲在窗台上晒太阳，我蹲下来它就往我腿边蹭",
            "我买了一小袋猫粮，结果引来四只，最后那只橘的吃最多，还打我",
            "这只猫耳朵上有个缺口，估计以前打过架，现在混得挺开，谁给吃的跟谁走",
            "有同学给它搭了个纸箱窝，第二天被人扔了，气得在群里骂了半小时",
            "提醒一下，别喂太咸的东西，上次有人喂火腿肠，猫拉了两天肚子",
            "它今天又在我书包上睡了一觉，毛沾了一书包，我也没舍得赶",
        ],
        "img": ["life_04"],
    },
    "sunset_walk": {
        "theme": "campus",
        "titles": ["今天的晚霞", "傍晚散步随手拍", "操场边上的天空", "这种天色只有秋天有", "教学楼后面的落日"],
        "lines": [
            "傍晚六点多从教学楼出来，整片天是橙红过渡到紫，我站着看了五分钟才想起拍照",
            "手机拍出来比肉眼差远了，颜色压得很平，凑合看",
            "沿着操场外圈走了一圈，风挺凉，穿短袖已经有点顶不住",
            "梧桐叶开始黄了，路灯一亮，地上全是碎影子",
            "这个点最适合发呆，什么都不想干，也不想回宿舍",
            "拍了十几张，只有这张勉强能发，其他全糊了",
        ],
        "img": ["campus_01", "campus_03", "life_03"],
    },
    "sports_field": {
        "theme": "campus",
        "titles": ["晨跑第五天", "操场晚上人真多", "体测又要来了", "跑道上的雾", "打球扭到脚了"],
        "lines": [
            "坚持晨跑第五天，今天早上有雾，跑道尽头看不太清，跑起来倒挺舒服",
            "晚上八点的操场全是人，跑道内圈被散步的占了，跑快一点就得一直绕",
            "体测通知下来了，引体向上我一个都做不了，已经开始焦虑",
            "跑步别穿板鞋，我上周穿板鞋跑了三公里，脚底板疼了四天",
            "打球的时候踩到别人脚，脚踝肿了一圈，冰敷完还是很疼，明天估计得去医院",
            "操场边上的自动售货机永远缺货，只有无糖茶，喝起来像草",
        ],
        "img": ["campus_04"],
    },
    "exam_week": {
        "theme": "campus",
        "titles": ["期末周的第一天", "复习不完了", "通宵自习警告", "考试范围到底在哪里", "考完第一门"],
        "lines": [
            "期末周正式开始，课表上还剩四门，我一门都没复习完",
            "老师说的重点和大家整理的重点完全不是一个东西，现在有五个版本在群里流传",
            "昨天通宵到三点，今天上课全程在飘，笔记写到一半变成了横线",
            "复习效率最高的时候是考前两小时，其余时间都在整理桌面",
            "第一门考完感觉还行，选择题全靠直觉，问答题把能想到的都写了",
            "建议别通宵，第二天脑子是空的，我试过，代价很大",
        ],
        "img": ["library_01", "library_03"],
    },
    "fps_match": {
        "theme": "game",
        "titles": ["今晚手感不错", "这把真的离谱", "终于摸到金了", "排位又连跪", "单人打了一把"],
        "lines": [
            "今晚打了六把，赢了四把，手感比上周好太多，主要是换了灵敏度",
            "刚刚那把在最后三十秒被人从背后偷了，我全程没听到脚步，回放看了三遍",
            "摸到金的时候手都在抖，撤出来那一刻心跳一百二",
            "排位连跪五把，队友全程各玩各的，我索性关麦自己打",
            "这游戏最上头的不是赢，是差一点就赢",
            "新手别急着冲，先把地图记熟，不然你会像我一样在出生点转圈",
        ],
        "img": ["game_01", "game_02"],
    },
    "teammate_troll": {
        "theme": "game",
        "titles": ["遇到这种队友怎么办", "开黑遇到奇葩", "匹配到的队友真行", "队友把我卖了", "这局纯靠自己"],
        "lines": [
            "开局他就把唯一的医疗包拿走了，中途还一直喊我给他掩护",
            "我死了之后他在旁边舔包舔了半分钟，最后被人家一枪带走，我也没觉得可惜",
            "双排的时候他全程在聊别的，报点报错两次，我直接闭麦",
            "这游戏真的需要沟通，但有些人的沟通就是指挥别人",
            "最后我一个人守住了，队友在语音里一句“哥牛”，我气就消了一半",
            "建议找固定队友，随机匹配太考验心态",
        ],
        "img": ["game_02", "game_03"],
    },
    "ping_lag": {
        "theme": "game",
        "titles": ["宿舍网又卡了", "延迟一百二怎么玩", "断线重连三次", "这网速真的没救", "打游戏必须开加速器"],
        "lines": [
            "晚上八点到十点，宿舍网必卡，延迟从三十跳到一百五，人物走两步退一步",
            "断线重连三次，第四次直接被判挂机，信誉分掉了不少",
            "换了根网线好了一点，但高峰期还是抖，估计是整栋楼在共用带宽",
            "开加速器能压到六十左右，就是每个月要交钱，学生党有点心疼",
            "打这种需要反应的游戏，延迟比配置重要，我算是体会到了",
            "建议错峰，晚上十一点之后反而稳，可能是因为大家都去睡了",
        ],
        "img": ["game_02", "desk_01"],
    },
    "gear_setup": {
        "theme": "game",
        "titles": ["桌面终于收拾好了", "换了新鼠标垫", "这套外设值不值", "灯光调了一晚上", "毕业前最后一次折腾"],
        "lines": [
            "折腾了一晚，桌面终于像样了，显示器抬高一点脖子舒服很多",
            "新鼠标垫是大号的，键盘鼠标都能放上去，滑起来比以前顺",
            "这套加起来花了不到六百，键盘是二手的，鼠标垫打折买的，性价比还行",
            "灯带贴在显示器后面，晚上关灯的时候氛围感确实有，就是有点晃眼",
            "毕业前最后一次折腾，之后这些东西要么带走要么卖，想着有点舍不得",
            "说真的，外设对水平提升有限，但心情提升很大",
        ],
        "img": ["game_04", "desk_03", "desk_01"],
    },
    "singleplayer": {
        "theme": "game",
        "titles": ["单机通关了", "这个剧情我哭了", "通关二十小时", "结局有点意难平", "重玩了一遍"],
        "lines": [
            "断断续续玩了二十多个小时，昨晚终于通关，结局那段我看了两遍",
            "剧情不算新鲜，但人物写得好，最后一个镜头我愣了很久",
            "这游戏的解谜设计挺讲究，卡了我三次，每次想通都挺爽",
            "重玩一遍才发现前面埋了很多伏笔，第一遍完全没注意",
            "单机的好处是不用等队友，坏处是通关之后有点空",
            "打折的时候买的，三十多块，值这个价",
        ],
        "img": ["game_01"],
    },
    "mobile_play": {
        "theme": "game",
        "titles": ["手机上也能玩", "躺床上打了两小时", "手游手感真不行", "手机发热严重", "通勤路上打一把"],
        "lines": [
            "手机上操作确实别扭，瞄准全靠手指，打了两把就放弃了",
            "躺床上打了两小时，手腕酸得不行，还是得坐着玩",
            "手机玩这类游戏发热很厉害，玩十分钟就烫手，还会掉帧",
            "通勤路上打一把挺解压，就是信号不稳，地铁里经常掉线",
            "手机屏幕小，远处的人根本看不清，全靠猜",
            "建议配个散热背夹，几十块钱，效果比想象中好",
        ],
        "img": ["game_03"],
    },
    "laptop_buy": {
        "theme": "digital",
        "titles": ["选笔记本求建议", "这个配置够用吗", "买轻薄本还是游戏本", "开学前买了电脑", "二手笔记本靠谱吗"],
        "lines": [
            "预算五千左右，主要写代码和看视频，偶尔打游戏，在纠结轻薄本还是游戏本",
            "看了几款，同价位游戏本显卡好但重，轻薄本续航好但散热差，很难两全",
            "写代码其实不需要太高的配置，内存加到十六G比什么都管用",
            "最后买了轻薄本，重量一点三公斤，通勤背着不累，就是风扇一开声音大",
            "二手的话建议当面验机，重点看电池循环次数和屏幕有没有暗斑",
            "别信“全新未拆”的低价，八成是翻新，这个坑我踩过",
        ],
        "img": ["desk_04", "desk_02"],
    },
    "phone_use": {
        "theme": "digital",
        "titles": ["换手机了", "这个续航真顶", "手机拍照对比", "旧手机还能再战", "买手机别冲动"],
        "lines": [
            "用三年的旧手机终于换了，主要是电池不行，一天要充两次",
            "新机续航确实好，中度使用能撑一天半，充电也快，半小时到七成",
            "拍照比想象中好，白天没问题，晚上还是有点糊，日常够用",
            "旧手机留着当备用机，装个卡当热点，也算物尽其用",
            "建议别在首发的时候买，等两个月价格能下来不少",
            "买之前先去实体店摸一下，手感和重量网上看不出来",
        ],
        "img": ["desk_02"],
    },
    "keyboard_feel": {
        "theme": "digital",
        "titles": ["换了把键盘", "青轴真的吵", "键帽换了之后", "键盘手感差异好大", "宿舍用机械键盘合适吗"],
        "lines": [
            "从薄膜换到机械，第一天打字速度反而变慢了，一周之后才习惯",
            "青轴声音太大，室友已经抗议两次，我准备换静音轴",
            "换了套键帽，手感没变多少，但看着舒服，这钱花得有点虚",
            "同样的轴体，不同键帽高度手感差别挺明显，这个之前完全没想过",
            "宿舍里建议红轴或者静音轴，青轴真的会得罪人",
            "键盘这种外设，适合自己的就是最好的，别人推荐只能参考",
        ],
        "img": ["desk_03"],
    },
    "headphone_pick": {
        "theme": "digital",
        "titles": ["耳机求推荐", "一百多的耳机够用吗", "降噪真的有用", "入耳戴久了耳朵疼", "宿舍必备耳机"],
        "lines": [
            "预算两百以内，主要通勤和自习用，纠结入耳还是头戴",
            "头戴隔音好但夏天闷，入耳方便但戴久了耳朵疼，各有各的问题",
            "自习室用降噪耳机效果挺明显，翻书声和键盘声基本听不到",
            "一百多的耳机日常够用，追求音质得加预算，但学生党没必要",
            "建议先试戴，每个人耳道不一样，别人说舒服你不一定",
            "宿舍里耳机是必需品，不然你会听到所有你想不到的声音",
        ],
        "img": ["game_04"],
    },
    "build_repair": {
        "theme": "digital",
        "titles": ["自己拆机清灰", "笔记本风扇太吵", "加装内存条", "硅脂换了之后", "不敢自己动手"],
        "lines": [
            "风扇声音大得像起飞，拆开一看全是灰，清了之后安静不少",
            "加了一条内存，从八G到十六G，开几个网页终于不卡了",
            "换硅脂要注意别涂太多，我第一次涂多了，温度反而高了两度",
            "拆机前记得先断电拔电池，我第一次没拔，螺丝刀碰到主板闪了一下，吓死",
            "保修期内建议别自己拆，我就是拆完发现不保修了",
            "工具一套三十块，比送修划算，前提是你敢动手",
        ],
        "img": ["desk_04"],
    },
    "kaoyan": {
        "theme": "study",
        "titles": ["考研倒计时五十天", "复习进度汇报", "要不要二战", "专业课背不完", "考研人的一天"],
        "lines": [
            "倒计时五十天，专业课还有两章没背，英语真题做到第三遍还是在错同一类题",
            "每天六点半起，晚上十一点回，中间有效学习时间大概七个小时，剩下都在走神",
            "最近一直在想如果没考上怎么办，家里希望我再来一年，我自己有点犹豫",
            "专业课的背诵量比想象中大，第一遍背完感觉什么都没记住",
            "自习室里大家都很安静，偶尔有人叹气，那种氛围挺压抑也挺鼓励",
            "建议每天留一小时运动，不然到后期身体先垮",
        ],
        "img": ["library_01", "library_02", "library_03"],
    },
    "internship": {
        "theme": "study",
        "titles": ["实习第三周", "通勤两小时值得吗", "实习都在干什么", "实习工资够花吗", "要不要继续实习"],
        "lines": [
            "实习第三周，主要在做接口联调和改 bug，代码量不大，但学到了不少规范",
            "每天通勤来回两小时，早上六点半出门，晚上八点到宿舍，人确实有点疲",
            "带我的师傅挺耐心，我提的问题再基础他也会讲，这点挺感激",
            "实习工资一天一百二，扣掉吃饭和通勤，基本不剩，主要是攒经验",
            "纠结要不要继续，学校那边还有课，两边都顾着有点吃不消",
            "建议实习前先把基础打牢，不然进去主要是看文档，成长很慢",
        ],
        "img": ["desk_01", "library_04"],
    },
    "resume": {
        "theme": "study",
        "titles": ["简历改了第八遍", "简历太空怎么办", "项目怎么写才不虚", "秋招简历求看", "简历被已读了"],
        "lines": [
            "简历改了八遍，删掉了所有“熟悉”“了解”，改成具体做过什么，看着踏实多了",
            "项目只有一个课设，写出来总觉得单薄，不知道要不要包装一下",
            "写项目建议按“问题—做法—结果”写，别只堆技术名词，面试官一问就露",
            "投了三十家，已读的不到十家，回信两家，心态有点崩",
            "实习经历是硬门槛，没有的话就把课设做深，至少能讲清楚",
            "建议找同学互看简历，自己看永远看不出问题",
        ],
        "img": ["desk_01", "library_04"],
    },
    "interview": {
        "theme": "study",
        "titles": ["面试被问懵了", "一面过了", "八股文背不完", "面试官人挺好", "二面挂了"],
        "lines": [
            "面试被问到项目里为什么用 Redis，我讲了一堆缓存，他说那不加行不行，我卡住了",
            "一面感觉一般，手撕代码写出来了但边界没处理好，居然过了",
            "八股文真的背不完，昨天背的今天忘，感觉在跟自己对抗",
            "面试官挺耐心，我答错的地方他会引导，聊完反而学到东西",
            "二面挂在场景题上，问我并发下怎么保证幂等，我只答了一半",
            "建议面试前把项目里每个技术选型的原因想清楚，这个比背题重要",
        ],
        "img": ["desk_01", "library_04"],
    },
    "course_project": {
        "theme": "study",
        "titles": ["课设终于跑起来了", "微服务拆分踩坑", "答辩PPT做完了", "联调跑不通", "项目部署到服务器"],
        "lines": [
            "课设终于跑起来了，五个服务加网关，启动顺序错了就各种 503，折腾了一晚上",
            "拆分微服务的时候最麻烦的是用户信息，内容服务要调用户服务，还要处理降级",
            "答辩 PPT 做完了，二十页，讲稿还没写，估计要练三四遍",
            "前后端联调跑不通，最后发现是跨域配置写漏了一行，找了两个小时",
            "部署到服务器之后才发现本地能跑不代表线上能跑，端口和防火墙都是坑",
            "建议早点开始，最后一周才开始的话基本是在赌运气",
        ],
        "img": ["desk_01", "library_04"],
    },
    "cet_exam": {
        "theme": "study",
        "titles": ["四级又没过", "六级听力太难", "背单词的第30天", "四级考试经验", "这次应该能过"],
        "lines": [
            "四级第二次考，还是差了几分，听力部分实在跟不上",
            "六级听力像在放rap，语速快还带口音，我只能抓关键词",
            "背单词坚持三十天，每天五十个，忘掉的比记住的多",
            "四级建议先做真题，把题型摸熟，比背单词提分快",
            "这次考前做了五套真题，感觉比上次稳，希望能过",
            "英语这东西停一个月就废，别指望突击",
        ],
        "img": ["library_02"],
    },
    "guilin_noodle": {
        "theme": "food",
        "titles": ["今天这碗米粉", "桂林米粉哪家强", "加卤蛋还是加锅烧", "米粉又涨价了", "早餐吃粉的第N天"],
        "lines": [
            "今天这碗加了锅烧和卤蛋，一共十一块，汤头够味，酸笋放得刚好",
            "学校后门那家我吃了两年，老板记得我不要葱，加汤的时候会多给一勺",
            "个人口味：卤菜粉加锅烧，酸豆角多放，辣椒少放，最后一定要加一勺卤水",
            "最近涨了一块，量倒是没变，也能接受",
            "外地朋友第一次吃可能受不了酸笋的味道，建议先少放一点",
            "早餐吃粉已经成习惯，一天不吃总觉得少点什么",
        ],
        "img": ["food_02"],
    },
    "canteen_new": {
        "theme": "food",
        "titles": ["食堂上新了", "今天这个菜不错", "食堂的隐藏吃法", "新窗口排队", "这个菜千万别点"],
        "lines": [
            "三楼新开了一个窗口，卖砂锅和炒饭，今天试了砂锅饭，量足味道也还行",
            "二楼的红烧肉今天炖得很烂，就是有点咸，配米饭刚好",
            "隐藏吃法是打一份白饭加两块钱的蒸蛋，再要一勺免费的汤，一共不到五块",
            "新窗口今天排了二十分钟，主要现做比较慢，味道对得起等待",
            "那个新出的凉拌菜别点，醋放太多，吃两口就腻",
            "食堂整体水平在涨，比以前好，就是价格也涨了",
        ],
        "img": ["food_01"],
    },
    "milk_tea": {
        "theme": "food",
        "titles": ["今天这杯奶茶", "奶茶店排队半小时", "无糖真的不甜", "又喝了一杯", "奶茶续命"],
        "lines": [
            "校门口那家新店，第二杯半价，排了半个小时，喝完觉得不太值",
            "我一般点三分糖去冰，甜度刚好，全糖真的齁",
            "无糖选项其实是少糖，喝起来还是有甜味，不过热量确实低一些",
            "这周已经喝了四杯，钱包和体重都在抗议",
            "推荐加椰果不加珍珠，珍珠太顶，喝完一下午不想吃饭",
            "奶茶这东西，喝的时候快乐，喝完有点后悔，第二天还是想喝",
        ],
        "img": ["food_03"],
    },
    "convenience": {
        "theme": "food",
        "titles": ["凌晨的便利店", "便利店关东煮", "半夜饿了", "便利店的东西真贵", "楼下便利店换老板了"],
        "lines": [
            "凌晨一点去楼下便利店，只有关东煮和泡面，收银小哥在打瞌睡",
            "关东煮的萝卜煮得很透，汤也好喝，就是有点贵，一串三块",
            "半夜饿了不想出门，翻遍宿舍只有一包泡面和一袋饼干",
            "同样的水便利店比超市贵一块五，但胜在近，半夜也只能认了",
            "换老板之后关东煮味道变了，汤淡了不少，我去得少了",
            "便利店是深夜唯一还亮着的地方，站在门口会有种奇怪的安全感",
        ],
        "img": ["life_01"],
    },
    "night_run": {
        "theme": "food",
        "titles": ["夜跑第一天", "晚上操场跑步", "跑步真的会上瘾", "跑完吃了个夜宵", "坚持一个月的变化"],
        "lines": [
            "夜跑第一天，跑了三公里，配速七分半，腿有点酸但心情很好",
            "晚上九点的操场风很凉，跑完在跑道边坐着，感觉一天的疲惫都散了",
            "跑步确实会上瘾，一天不跑就难受，但膝盖也开始提醒我别太猛",
            "跑完顺手吃了个夜宵，等于白跑，这个循环我已经持续两周",
            "坚持一个月，体重掉了三斤，睡眠好了很多，这个收益比想象中大",
            "建议买双正经跑鞋，我用板鞋跑了两周，脚底板疼得下不了楼",
        ],
        "img": ["campus_04"],
    },
    "weather": {
        "theme": "food",
        "titles": ["今天降温了", "这雨下了一整天", "桂林的天气", "秋天来了", "潮湿到怀疑人生"],
        "lines": [
            "今天突然降温，早上穿短袖出门，中午冻得去买了一件外套",
            "雨从早上一直下到晚上，鞋全湿了，晾在窗边一晚上没干",
            "桂林的天气就是四季随机播放，早上冷中午热，衣服永远穿不对",
            "秋天来了，梧桐叶开始黄，晚上不开风扇也能睡着",
            "回南天最难受，墙壁出水，衣服晾三天还是潮的，整个人也跟着发霉",
            "提醒一下，这个季节容易感冒，宿舍里已经有两个人中招",
        ],
        "img": ["campus_02", "life_03"],
    },
}

THEME_WEIGHTS = {"campus": 35, "game": 20, "digital": 15, "study": 15, "food": 15}

TITLE_ENDINGS = [
    "，谁懂啊", "，在线等", "，记录一下", "，求个建议", "，有点破防",
    "，说说你们那边", "，先记下来", "，笑不活了", "，真的会谢", "，人麻了",
    "，随便聊聊", "，有感而发",
]

TAGS = {
    "campus": ["#校园日常", "#早八人", "#图书馆", "#宿舍生活", "#晚霞", "#校园猫"],
    "game": ["#三角洲行动", "#开黑", "#单机游戏", "#排位", "#游戏日常"],
    "digital": ["#数码", "#装机", "#外设", "#笔记本", "#二手捡漏"],
    "study": ["#考研", "#实习", "#秋招", "#课设", "#四六级"],
    "food": ["#桂林米粉", "#食堂", "#奶茶", "#夜宵", "#夜跑"],
}

# 场景级话题标签（避免“早八帖挂 #晚霞”这种错配）
SCENE_TAGS = {
    "library_seat": ["#图书馆", "#自习"],
    "canteen_queue": ["#食堂", "#干饭"],
    "dorm_life": ["#宿舍生活", "#断电"],
    "early_class": ["#早八人", "#校园日常"],
    "express_station": ["#快递", "#校园日常"],
    "campus_cat": ["#校园猫", "#撸猫"],
    "sunset_walk": ["#晚霞", "#随手拍"],
    "sports_field": ["#夜跑", "#操场"],
    "exam_week": ["#期末周", "#复习"],
    "fps_match": ["#三角洲行动", "#排位"],
    "teammate_troll": ["#开黑", "#队友"],
    "ping_lag": ["#校园网", "#延迟"],
    "gear_setup": ["#外设", "#桌面"],
    "singleplayer": ["#单机游戏", "#剧情"],
    "mobile_play": ["#手游", "#通勤"],
    "laptop_buy": ["#笔记本", "#选购"],
    "phone_use": ["#数码", "#手机"],
    "keyboard_feel": ["#键盘", "#外设"],
    "headphone_pick": ["#耳机", "#降噪"],
    "build_repair": ["#拆机", "#清灰"],
    "kaoyan": ["#考研", "#自习"],
    "internship": ["#实习", "#打工人"],
    "resume": ["#简历", "#秋招"],
    "interview": ["#面试", "#秋招"],
    "course_project": ["#课设", "#微服务"],
    "cet_exam": ["#四六级", "#英语"],
    "guilin_noodle": ["#桂林米粉", "#早餐"],
    "canteen_new": ["#食堂", "#新菜"],
    "milk_tea": ["#奶茶", "#续命"],
    "convenience": ["#便利店", "#夜宵"],
    "night_run": ["#夜跑", "#减肥"],
    "weather": ["#天气", "#秋天"],
}

# 评论语料
COMMENT_GENERIC = [
    "第一", "沙发", "笑死我了", "这也行", "？", "绝了", "我居然看完了",
    "蹲个后续", "楼主说的是真的吗", "已收藏", "羡慕了", "同城，有空约一下",
    "看完心情复杂", "这波我站楼主", "有点东西", "路过，顶一下",
    "哈哈哈哈哈哈", "所以你打算怎么办", "说得好，但是没用", "我懂这种感觉",
]

COMMENT_BY_THEME = {
    "campus": [
        "同款经历，上周我也遇到", "四楼确实一直满", "我们宿舍也这样，无解",
        "建议直接找辅导员", "这个时间段人最多", "换我我也受不了",
        "图书馆三楼人少，可以试试", "拍得挺好看", "这个天气确实舒服",
    ],
    "game": [
        "这枪法可以啊", "求带", "队友问题，不是你的问题", "灵敏度多少？",
        "这游戏现在平衡性太差", "我昨天也在这图被偷", "开黑吗，我菜但听话",
        "延迟高真的没法玩", "换个加速器试试", "这操作我学不来",
    ],
    "digital": [
        "同款，用了一年没问题", "这个价位其实还有更好的", "二手要看电池",
        "内存加到16G是刚需", "键盘声音大是真的烦", "推荐具体型号？",
        "我用的也是这个，还行", "清灰确实有用，我上周刚清", "别买首发",
    ],
    "study": [
        "同备考，加油", "简历可以发我看看吗", "面试题都是这些，多练",
        "课设做完了不起", "这个项目讲清楚就行", "四级多刷真题真的有用",
        "实习经历确实重要", "我面了五家，只过了一家", "别焦虑，还有时间",
    ],
    "food": [
        "这家在哪？求定位", "看着就香", "食堂阿姨今天手抖了吗", "多少钱一份",
        "我常去，老板人挺好", "酸笋少放一点会更好吃", "半夜看饿了",
        "同款快乐", "这家我吃过，一般", "下次一起去",
    ],
}

COMMENT_REPLIES = [
    "回楼上：我昨天也去了，情况差不多",
    "真的假的，别骗我",
    "哈哈哈这条我笑了",
    "咱俩说的是一个地方吗",
    "+1，同感",
    "确实，我也这么觉得",
    "我补充一句，情况还分时间",
    "这个我不同意，我觉得还行",
    "笑死，你这也太真实了",
    "同问，蹲个答案",
    "这条评论比正文有意思",
    "所以结论是什么",
    "差不多是这样，但细节不一样",
    "我也是这么想的",
    "有道理，但我还是保留意见",
    "看人吧，我就没这感觉",
]

AUTHOR_REPLIES = [
    "统一回复一下：{}",
    "谢谢大家，已经在改了",
    "别催了别催了，明天就更新",
    "楼主在线，有问题直接问",
    "看到大家讨论这么热烈有点意外",
    "补充一点：{}",
    "你们说的我都看了，确实有道理",
    "这条我置顶一下",
    "{}, 就这样，不再回复了",
    "其实没大家想的那么复杂",
]

REPORT_REASONS = [
    "色情低俗", "不实信息", "引战辱骂", "垃圾广告", "侵犯隐私", "抄袭搬运",
]
REPORT_REMARKS = [
    "内容明显引战，评论区已经吵起来了",
    "这条是编的，学校根本没发过这个通知",
    "疑似广告，带联系方式",
    "图片不是本人拍的，搬运别人的",
    "言语攻击其他同学，看着很不舒服",
    "内容涉及隐私，未经同意就发了",
    "重复刷屏，同一个内容发了三次",
]


# ----------------------------------------------------------------------------
# 工具函数
# ----------------------------------------------------------------------------
def rng_pick_hour(rng):
    total = sum(w for _, w in HOUR_WEIGHTS)
    r = rng.uniform(0, total)
    acc = 0
    for h, w in HOUR_WEIGHTS:
        acc += w
        if r <= acc:
            return h
    return 22


def rand_time(rng, start, end):
    """在 [start, end) 内按小时权重取一个时间点"""
    if end <= start:
        return start + timedelta(minutes=rng.randint(1, 30))
    span = max(int((end - start).total_seconds()), 1)
    for _ in range(80):
        d = rng.randint(0, max(span // 86400, 1) - 1)
        t = start + timedelta(
            days=d,
            hours=rng_pick_hour(rng),
            minutes=rng.randint(0, 59),
            seconds=rng.randint(0, 59),
        )
        if t < end:
            return t
    return end - timedelta(minutes=rng.randint(1, 60))


def fmt_dt(t):
    return t.strftime("%Y-%m-%d %H:%M:%S") if t else None


def sql_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    s = str(v)
    s = s.replace("\\", "\\\\").replace("'", "''").replace("\r", "")
    s = s.replace("\n", "\\n")
    return "'" + s + "'"


class SqlWriter:
    def __init__(self):
        self.lines = []
        self.row_total = 0

    def delete_block(self, tables, user_ids=None):
        self.lines.append("-- ===== 清空旧数据（帖子与全部互动数据；user 只清 16~95 的种子用户） =====")
        for t in tables:
            self.lines.append("DELETE FROM %s;" % t)
        for t in tables:
            self.lines.append("ALTER TABLE %s AUTO_INCREMENT = 1;" % t)
        if user_ids:
            self.lines.append("DELETE FROM `user` WHERE id BETWEEN %d AND %d;" % user_ids)
            self.lines.append("ALTER TABLE `user` AUTO_INCREMENT = %d;" % (user_ids[1] + 1))
        self.lines.append("")

    def insert_batch(self, table, columns, rows, batch=200):
        if not rows:
            return
        col_sql = ", ".join("`%s`" % c for c in columns)
        for i in range(0, len(rows), batch):
            chunk = rows[i:i + batch]
            values = ",\n".join(
                "(" + ", ".join(sql_val(v) for v in r) + ")" for r in chunk
            )
            self.lines.append("INSERT INTO `%s` (%s) VALUES\n%s;" % (table, col_sql, values))
            self.row_total += len(chunk)
        self.lines.append("")

    def save(self, path):
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write("-- 由 seed.py 生成，请勿手工编辑\n")
            f.write("-- 生成时间：%s\n" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
            f.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS = 0;\n\n")
            f.write("\n".join(self.lines))
            f.write("SET FOREIGN_KEY_CHECKS = 1;\n")


# ----------------------------------------------------------------------------
# 图片生成
# ----------------------------------------------------------------------------
def load_pool():
    pool = {}
    for name in os.listdir(POOL_DIR):
        if not name.lower().endswith((".jpg", ".jpeg", ".png")):
            continue
        key = os.path.splitext(name)[0]
        pool[key] = os.path.join(POOL_DIR, name)
    return pool


def make_photo_variant(base_path, out_path, width, height, rng):
    im = Image.open(base_path).convert("RGB")
    bw, bh = im.size
    ratio = width / height
    max_w = min(bw, int(bh * ratio))
    cw = max(int(max_w * rng.uniform(0.72, 1.0)), 200)
    ch = max(int(cw / ratio), 200)
    x = rng.randint(0, max(bw - cw, 0))
    y = rng.randint(0, max(bh - ch, 0))
    im = im.crop((x, y, x + cw, y + ch)).resize((width, height), Image.LANCZOS)

    im = ImageEnhance.Brightness(im).enhance(rng.uniform(0.93, 1.07))
    im = ImageEnhance.Contrast(im).enhance(rng.uniform(0.92, 1.08))
    im = ImageEnhance.Color(im).enhance(rng.uniform(0.90, 1.10))
    im.save(out_path, "JPEG", quality=rng.randint(80, 88), optimize=True)


def make_avatar(text, out_path, rng):
    size = 200
    palettes = [
        ((59, 130, 246), (30, 64, 175)), ((16, 185, 129), (6, 95, 70)),
        ((244, 114, 182), (157, 23, 77)), ((251, 146, 60), (194, 65, 12)),
        ((139, 92, 246), (76, 29, 149)), ((20, 184, 166), (15, 118, 110)),
        ((234, 179, 8), (161, 98, 7)), ((100, 116, 139), (51, 65, 85)),
    ]
    c1, c2 = palettes[rng.randint(0, len(palettes) - 1)]
    im = Image.new("RGB", (size, size), c1)
    d = ImageDraw.Draw(im)
    for y in range(size):
        k = y / size
        d.line([(0, y), (size, y)],
               fill=(int(c1[0] * (1 - k) + c2[0] * k),
                     int(c1[1] * (1 - k) + c2[1] * k),
                     int(c1[2] * (1 - k) + c2[2] * k)))
    # 圆形遮罩
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((4, 4, size - 4, size - 4), fill=255)
    im.putalpha(mask)

    ch = text[0] if text else "U"
    font = None
    for fp in (r"C:\Windows\Fonts\msyhbd.ttc", r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\simhei.ttf"):
        if os.path.exists(fp):
            try:
                font = ImageFont.truetype(fp, 96)
                break
            except Exception:
                continue
    if font is None:
        font = ImageFont.load_default()

    d = ImageDraw.Draw(im)
    try:
        bbox = d.textbbox((0, 0), ch, font=font)
        w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
        d.text(((size - w) / 2 - bbox[0], (size - h) / 2 - bbox[1]), ch, fill=(255, 255, 255), font=font)
    except Exception:
        d.text((size / 2, size / 2), ch, fill=(255, 255, 255), font=font, anchor="mm")
    im.save(out_path, "PNG")


def unique_photo_name(rng, when, used):
    for _ in range(30):
        name = "%d_%d.jpg" % (int(when.timestamp()) + rng.randint(0, 900),
                             rng.randint(1000, 9999))
        if name not in used:
            used.add(name)
            return name
    name = "%d_%d.jpg" % (int(when.timestamp()), rng.randint(100000, 999999))
    used.add(name)
    return name


# ----------------------------------------------------------------------------
# 数据生成
# ----------------------------------------------------------------------------
def build_users(rng, pool):
    users = []
    used_accounts = set(LEGACY_ACCOUNTS)
    used_phones = set(LEGACY_PHONES)
    used_names = set(LEGACY_NAMES)

    names = NICKNAMES[:]
    rng.shuffle(names)

    # 历史账号注册时间更早，种子账号分布在 2026-01 ~ 2026-10
    reg_start = datetime(2026, 1, 5)
    reg_end = datetime(2026, 10, 30, 23, 0)

    for i in range(USER_COUNT):
        name = names[i % len(names)]
        users.append({
            "id": USER_ID_START + i,
            "username": name,
            "password": PASSWORD_HASH,
            "role": 1 if i in (7, 48) else 0,
            "user_account": None,
            "phone": None,
            "avatar": None,
            "cover": None,
            "bio": None,
            "birthday": None,
            "location": None,
            "create_time": rand_time(rng, reg_start, reg_end),
            "status": "0",
            "is_deleted": "0",
            "total_like_count": 0,
            "follow_count": 0,
            "fan_count": 0,
            "idx": i,
        })

    for u in users:
        while True:
            acc = str(rng.randint(20000000, 89999999))
            if acc not in used_accounts:
                used_accounts.add(acc)
                u["user_account"] = acc
                break
        while True:
            ph = "1%d%d%08d" % (rng.choice([3, 5, 7, 8, 9]), rng.randint(0, 9), rng.randint(0, 99999999))
            if ph not in used_phones:
                used_phones.add(ph)
                u["phone"] = ph
                break

        # 资料完整度：约 55% 有简介、70% 有生日、80% 有地区
        if rng.random() < 0.55:
            u["bio"] = rng.choice(BIOS) or None
        if rng.random() < 0.70:
            u["birthday"] = "%04d-%02d-%02d" % (rng.randint(1998, 2006), rng.randint(1, 12), rng.randint(1, 28))
        loc = rng.choice(LOCATIONS)
        if loc:
            u["location"] = loc

        av_name = u["user_account"] + ".png"
        u["avatar"] = "%s/avatar/%s" % (BASE_URL, av_name)
        u["avatar_file"] = av_name

    # 约 25% 用户有主页封面
    cover_imgs = ["campus_01", "campus_03", "life_03", "life_02", "campus_04", "food_03"]
    for u in users:
        if rng.random() < 0.25:
            base = pool[rng.choice(cover_imgs)]
            cv_name = "cover_%s.jpg" % u["user_account"]
            u["cover"] = "%s/cover/%s" % (BASE_URL, cv_name)
            u["cover_file"] = cv_name
            u["cover_base"] = base
    return users


def build_posts(rng, users):
    # 发帖活跃度：少数人贡献大部分内容，约 35% 用户从不发帖
    active = users[:]
    weights = []
    for u in active:
        r = rng.random()
        if r < 0.06:
            weights.append(14)      # 重度发帖
        elif r < 0.30:
            weights.append(4)
        elif r < 0.65:
            weights.append(2)
        else:
            weights.append(0.4)
    authors_pool = [(u, w) for u, w in zip(active, weights)]

    scenes = list(SCENES.keys())
    scene_weights = [THEME_WEIGHTS[SCENES[s]["theme"]] for s in scenes]

    topics = []
    used_titles = set()
    for i in range(POST_COUNT):
        author = rng.choices([a for a, _ in authors_pool], weights=[w for _, w in authors_pool])[0]
        scene = rng.choices(scenes, weights=scene_weights)[0]
        conf = SCENES[scene]

        core = rng.choice(conf["titles"])
        title = core + rng.choice(TITLE_ENDINGS)
        for _ in range(20):
            if title not in used_titles:
                break
            title = core + rng.choice(TITLE_ENDINGS)
            if title in used_titles:
                title = "%s%s（%d）" % (core, rng.choice(TITLE_ENDINGS), i)
        used_titles.add(title)
        if len(title) > 95:
            title = title[:95]

        lines = conf["lines"][:]
        rng.shuffle(lines)
        n = rng.choices([2, 3, 4], weights=[35, 45, 20])[0]
        body = [lines[j] for j in range(min(n, len(lines)))]

        if rng.random() < 0.55:
            tag = rng.choice(SCENE_TAGS.get(scene, TAGS[conf["theme"]]))
            if rng.random() < 0.5:
                body.append(tag)
            else:
                body[-1] = body[-1] + " " + tag

        content = "\n".join(body)

        start = NOW - timedelta(days=90)
        when = rand_time(rng, start, NOW - timedelta(minutes=30))
        topics.append({
            "author": author,
            "scene": scene,
            "theme": conf["theme"],
            "title": title,
            "content": content,
            "create_time": when,
            "media_type": 2 if rng.random() < 0.40 else 1,
            "like_users": [],
            "comments": [],
            "collect_users": [],
            "is_banned": False,
            "is_deleted": "0",
        })

    # 约 5% 帖子被封禁（其中一半来自举报处理，后面再定），2% 被逻辑删除
    banned = rng.sample(range(POST_COUNT), k=int(POST_COUNT * 0.05))
    for i in banned:
        topics[i]["is_banned"] = True
    deleted = rng.sample([i for i in range(POST_COUNT) if i not in banned], k=8)
    for i in deleted:
        topics[i]["is_deleted"] = "1"

    # 先按时间排序，再分配 id（真实自增表 id 与时间同向）
    topics.sort(key=lambda p: p["create_time"])
    for idx, p in enumerate(topics):
        p["id"] = idx + 1
    return topics


def build_images(rng, posts, pool, used_names, manifest):
    images = []
    img_id = 0
    for p in posts:
        if p["media_type"] != 2:
            continue
        n = rng.choices([1, 2, 3], weights=[62, 28, 10])[0]
        bases = SCENES[p["scene"]]["img"]
        for k in range(n):
            base_key = rng.choice(bases)
            base = pool[base_key]
            square = rng.random() < 0.15
            w, h = (1080, 1080) if square else (1200, 900)
            name = unique_photo_name(rng, p["create_time"], used_names)
            out = os.path.join(UPLOAD_NEWS, name)
            make_photo_variant(base, out, w, h, rng)
            manifest.append("uploads/news/" + name)
            img_id += 1
            images.append({
                "id": img_id,
                "news_id": p["id"],
                "image_url": "%s/news/%s" % (BASE_URL, name),
                "sort_order": k + 1,
                "create_time": p["create_time"] + timedelta(seconds=k * 3),
                "media_type": 1,
                "cover_url": None,
                "width": w,
                "height": h,
                "is_delete": 0,
            })
        for im in images:
            if im["news_id"] == p["id"] and im["sort_order"] == 1:
                im["cover_url"] = im["image_url"]

    # id 按 news_id、sort_order 顺序
    images.sort(key=lambda x: (x["news_id"], x["sort_order"]))
    for idx, im in enumerate(images):
        im["id"] = idx + 1
    return images


def post_like_count(rng):
    r = rng.random()
    if r < 0.35:
        return rng.randint(0, 2)
    if r < 0.75:
        return rng.randint(3, 7)
    if r < 0.92:
        return rng.randint(8, 16)
    if r < 0.98:
        return rng.randint(18, 35)
    return rng.randint(40, 70)


def build_likes(rng, posts, users):
    posts_by_id = {p["id"]: p for p in posts}
    likes = []
    for p in posts:
        target = post_like_count(rng)
        # 只能被注册早于发帖的用户点赞，且不能给自己点赞
        eligible = [u for u in users
                    if u["create_time"] < p["create_time"] and u["id"] != p["author"]["id"]]
        if not eligible:
            continue
        target = min(target, len(eligible))
        picked = rng.sample(eligible, k=target)
        for u in picked:
            when = rand_time(rng, p["create_time"] + timedelta(minutes=2),
                             NOW - timedelta(minutes=1))
            likes.append({
                "news_id": p["id"],
                "user_id": u["id"],
                "create_time": when,
                "_post": p,
                "_user": u,
            })
            p["like_users"].append(u)
    likes.sort(key=lambda x: x["create_time"])
    for idx, lk in enumerate(likes):
        lk["id"] = idx + 1
        lk["update_time"] = lk["create_time"]
    return likes


def comment_count_for_post(rng):
    r = rng.random()
    if r < 0.42:
        return 0
    if r < 0.70:
        return rng.randint(1, 2)
    if r < 0.90:
        return rng.randint(3, 6)
    if r < 0.98:
        return rng.randint(7, 14)
    return rng.randint(16, 30)


def build_comments(rng, posts, users):
    comments = []
    for p in posts:
        roots_target = comment_count_for_post(rng)
        if roots_target == 0:
            continue
        # 评论者池：点过赞的人在前，其余已注册用户补充（真实社区里大量评论者并不点赞）
        likers = [u for u in p["like_users"] if u["id"] != p["author"]["id"]]
        like_ids = {u["id"] for u in likers}
        others = [u for u in users
                  if u["id"] != p["author"]["id"] and u["id"] not in like_ids
                  and u["create_time"] < p["create_time"]]
        candidates_pool = likers + others
        used_root_ids = set()
        used_texts = set()
        pool_text = COMMENT_GENERIC + COMMENT_BY_THEME[p["theme"]]
        for _ in range(roots_target):
            avail_text = [t for t in pool_text if t not in used_texts] or pool_text
            text = rng.choice(avail_text)
            if p["theme"] == "game" and rng.random() < 0.3:
                text = text + " 开黑吗"
            used_texts.add(text)
            avail = [u for u in candidates_pool if u["id"] not in used_root_ids] or candidates_pool
            commenter = rng.choice(avail) if avail else p["author"]
            used_root_ids.add(commenter["id"])
            when = rand_time(rng, p["create_time"] + timedelta(minutes=3),
                             min(NOW - timedelta(minutes=1),
                                 p["create_time"] + timedelta(days=12)))
            root = {
                "news_id": p["id"],
                "user_id": commenter["id"],
                "parent_id": 0,
                "to_user_id": None,
                "root_ref": None,
                "content": text,
                "status": 1,
                "create_time": when,
                "dislike": 0,
                "likes": [],
                "depth": 0,
                "parent_obj": None,
                "_author": commenter,
                "_post": p,
            }
            comments.append(root)
            p["comments"].append(root)

            # 楼中楼：30% 的根评论有回复，回复本身 25% 概率再被回复
            if rng.random() < 0.32:
                replies = rng.randint(1, 2)
                last = [root]
                for d in range(replies):
                    parent = rng.choice(last)
                    pool_author = parent["_author"]
                    # 28% 由楼主亲自下场，用楼主口径；其余从评论者池里找非当事人
                    if rng.random() < 0.28:
                        replier = p["author"]
                        rep_name = rng.choice(AUTHOR_REPLIES).format(
                            rng.choice(["时间不固定", "具体情况看人", "我这边也是这样", "下周再说"]))
                    else:
                        cand = [u for u in candidates_pool
                                if u["id"] not in (parent["user_id"], p["author"]["id"])]
                        replier = rng.choice(cand) if cand else p["author"]
                        rep_name = rng.choice(COMMENT_REPLIES)
                        if rng.random() < 0.30:
                            rep_name = "回复 @" + pool_author["username"] + "：" + rep_name
                    when2 = rand_time(rng, parent["create_time"] + timedelta(minutes=2),
                                      min(NOW - timedelta(minutes=1),
                                          parent["create_time"] + timedelta(days=3)))
                    child = {
                        "news_id": p["id"],
                        "user_id": replier["id"],
                        "parent_id": None,
                        "to_user_id": parent["user_id"],
                        "root_ref": root,
                        "content": rep_name,
                        "status": 1,
                        "create_time": when2,
                        "dislike": 0,
                        "likes": [],
                        "depth": d + 1,
                        "parent_obj": parent,
                        "_author": replier,
                        "_post": p,
                    }
                    comments.append(child)
                    p["comments"].append(child)
                    last.append(child)

    # 少量评论被删除（作者删或管理员下架），约 2%
    for c in comments:
        if rng.random() < 0.02:
            c["status"] = 0

    comments.sort(key=lambda c: c["create_time"])
    for idx, c in enumerate(comments):
        c["id"] = idx + 1
    for c in comments:
        if c["depth"] == 0:
            c["root_id"] = None
            c["parent_id"] = 0
        else:
            c["root_id"] = c["root_ref"]["id"]
            c["parent_id"] = c["parent_obj"]["id"] if c["parent_obj"] else 0
    return comments


def build_comment_likes(rng, comments):
    likes = []
    for c in comments:
        if c["status"] != 1:
            continue
        if rng.random() < 0.35:
            n = rng.choices([1, 2, 3, 5, 8], weights=[45, 30, 15, 8, 2])[0]
            candidates = [u for u in c["_post"]["like_users"] if u["id"] != c["user_id"]]
            if not candidates:
                continue
            n = min(n, len(candidates))
            for u in rng.sample(candidates, k=n):
                when = rand_time(rng, c["create_time"] + timedelta(minutes=1),
                                 NOW - timedelta(minutes=1))
                likes.append({
                    "comment_id": c["id"],
                    "user_id": u["id"],
                    "create_time": when,
                    "_comment": c,
                })
                c["likes"].append(u)
    likes.sort(key=lambda x: x["create_time"])
    for idx, lk in enumerate(likes):
        lk["id"] = idx + 1
        lk["update_time"] = lk["create_time"]
    return likes


def build_dislikes(rng, comments):
    dislikes = []
    for c in comments:
        if c["status"] != 1:
            continue
        # 约 2.5% 的评论被踩，1~3 人
        if rng.random() < 0.025:
            n = rng.randint(1, 3)
            candidates = [u for u in c["_post"]["like_users"] if u["id"] != c["user_id"]]
            if not candidates:
                continue
            for u in rng.sample(candidates, k=min(n, len(candidates))):
                when = rand_time(rng, c["create_time"] + timedelta(minutes=3),
                                 NOW - timedelta(minutes=1))
                dislikes.append({
                    "comment_id": c["id"],
                    "user_id": u["id"],
                    "create_time": when,
                    "_comment": c,
                })
            c["dislike"] = len(c.get("_dislikes", []))
    dislikes.sort(key=lambda x: x["create_time"])
    for idx, d in enumerate(dislikes):
        d["id"] = idx + 1
        d["update_time"] = d["create_time"]
    return dislikes


def build_follows(rng, users):
    # 兴趣圈：按用户常发内容的主题给他们打标签
    pool_by_theme = {t: [] for t in THEME_WEIGHTS}
    for u in users:
        u["_tags"] = rng.sample(list(THEME_WEIGHTS), k=rng.randint(1, 3))  # 用 list，保证 set 迭代顺序不引入随机
        for t in u["_tags"]:
            pool_by_theme[t].append(u)

    edges = []
    for u in users:
        target_n = rng.choices([0, 2, 5, 9, 15, 22], weights=[10, 20, 30, 25, 11, 4])[0]
        candidates = []
        for t in u["_tags"]:
            candidates.extend(pool_by_theme[t])
        candidates = [c for c in candidates if c["id"] != u["id"]]
        if not candidates:
            continue
        random_pool = [x for x in users if x["id"] != u["id"]]
        picked = set()
        for _ in range(target_n):
            if rng.random() < 0.7:
                c = rng.choice(candidates)
            else:
                c = rng.choice(random_pool)
            if c["id"] == u["id"] or c["id"] in picked:
                continue
            picked.add(c["id"])
            edges.append({
                "user_id": u["id"],
                "follow_user_id": c["id"],
                "create_time": rand_time(rng, max(u["create_time"], c["create_time"]),
                                         NOW - timedelta(minutes=1)),
                "_from": u,
                "_to": c,
            })
    # 互关：15% 的关注被回关
    pair_set = {(e["user_id"], e["follow_user_id"]) for e in edges}
    for e in list(edges):
        if rng.random() < 0.15:
            back = (e["follow_user_id"], e["user_id"])
            if back in pair_set:
                continue
            pair_set.add(back)
            edges.append({
                "user_id": e["follow_user_id"],
                "follow_user_id": e["user_id"],
                "create_time": e["create_time"] + timedelta(minutes=rng.randint(1, 200)),
                "_from": e["_to"],
                "_to": e["_from"],
            })
    edges.sort(key=lambda x: x["create_time"])
    for idx, e in enumerate(edges):
        e["id"] = idx + 1
        e["update_time"] = e["create_time"]
        e["_from"]["follow_count"] += 1
        e["_to"]["fan_count"] += 1
    return edges


def build_collects(rng, posts, users):
    rows = []
    by_id = {p["id"]: p for p in posts}
    for u in users:
        liked = [p for p in posts if u in p["like_users"]]
        if not liked:
            continue
        n = rng.choices([0, 2, 5, 9, 14], weights=[8, 20, 30, 25, 17])[0]
        if n <= 0:
            continue
        for p in rng.sample(liked, k=min(n, len(liked))):
            when = rand_time(rng, p["create_time"] + timedelta(minutes=5), NOW - timedelta(minutes=1))
            rows.append({
                "user_id": u["id"],
                "collect_type": 1,
                "target_id": p["id"],
                "create_time": when,
                "_post": p,
            })
            p["collect_users"].append(u)
        # 少量用户收藏评论
        if rng.random() < 0.08:
            comments = [c for c in p["comments"] if c["status"] == 1]
            if comments:
                c = rng.choice(comments)
                rows.append({
                    "user_id": u["id"],
                    "collect_type": 2,
                    "target_id": c["id"],
                    "create_time": rand_time(rng, c["create_time"] + timedelta(minutes=5),
                                             NOW - timedelta(minutes=1)),
                    "_post": None,
                })
    rows.sort(key=lambda x: x["create_time"])
    for idx, r in enumerate(rows):
        r["id"] = idx + 1
        r["update_time"] = r["create_time"]
    # collect_count 按帖子回算
    for p in posts:
        p["collect_count"] = len([1 for u in p["collect_users"]])
    return rows


def build_disliked_posts(rng, posts, users):
    rows = []
    for u in users:
        if rng.random() < 0.30:
            n = rng.randint(1, 3)
            for p in rng.sample(posts, k=min(n, len(posts))):
                rows.append({
                    "post_id": p["id"],
                    "user_id": u["id"],
                    "create_time": rand_time(rng, p["create_time"], NOW - timedelta(minutes=1)),
                })
    rows.sort(key=lambda x: x["create_time"])
    for idx, r in enumerate(rows):
        r["id"] = idx + 1
        r["update_time"] = r["create_time"]
    return rows


def build_reports(rng, posts, comments, users):
    rows = []
    banned_posts = [p for p in posts if p["is_banned"]]
    normal_posts = [p for p in posts if not p["is_banned"] and p["is_deleted"] == "0"]

    # 10 条针对封禁帖的“已通过”举报
    for p in banned_posts[:10]:
        reporter = rng.choice([u for u in users if u["id"] != p["author"]["id"]])
        rows.append({
            "report_type": 1,
            "target_id": p["id"],
            "reporter": reporter,
            "reason_type": rng.choice(REPORT_REASONS),
            "remark": rng.choice(REPORT_REMARKS),
            "status": 1,
            "create_time": rand_time(rng, p["create_time"] + timedelta(hours=2),
                                     NOW - timedelta(minutes=10)),
            "_post": p,
            "_comment": None,
        })

    # 20 条针对正常帖的举报：前 8 条已驳回，后 12 条待审核（同一批抽样，避免重复举报同一帖）
    pick_pool = rng.sample(normal_posts, k=min(20, len(normal_posts)))
    for i, p in enumerate(pick_pool):
        reporter = rng.choice([u for u in users if u["id"] != p["author"]["id"]])
        status = 2 if i < 8 else 0
        rows.append({
            "report_type": 1,
            "target_id": p["id"],
            "reporter": reporter,
            "reason_type": rng.choice(REPORT_REASONS),
            "remark": rng.choice(REPORT_REMARKS),
            "status": status,
            "create_time": rand_time(rng, p["create_time"] + timedelta(hours=2),
                                     NOW - timedelta(minutes=10)),
            "_post": p,
            "_comment": None,
        })

    # 5 条评论举报：2 已处理（评论 status=0）、3 待审核
    comment_pool = [c for c in comments if c["status"] == 1]
    picked = rng.sample(comment_pool, k=min(5, len(comment_pool)))
    for i, c in enumerate(picked):
        reporter = rng.choice([u for u in users if u["id"] != c["user_id"]])
        status = 1 if i < 2 else 0
        rows.append({
            "report_type": 2,
            "target_id": c["id"],
            "reporter": reporter,
            "reason_type": rng.choice(REPORT_REASONS),
            "remark": rng.choice(REPORT_REMARKS),
            "status": status,
            "create_time": rand_time(rng, c["create_time"] + timedelta(hours=3),
                                     NOW - timedelta(minutes=10)),
            "_post": c["_post"],
            "_comment": c,
        })
        if status == 1:
            c["status"] = 0

    rows.sort(key=lambda x: x["create_time"])
    for idx, r in enumerate(rows):
        r["id"] = idx + 1
        r["update_time"] = r["create_time"]
        r["user_id"] = r["reporter"]["id"]
    return rows


def build_notifications(rng, reports, posts):
    rows = []
    for r in reports:
        if r["status"] == 1:
            if r["report_type"] == 1:
                p = r["_post"]
                rows.append({
                    "user_id": p["author"]["id"],
                    "type": 1,
                    "content": "你的帖子《%s》因违反社区规定已被下架" % p["title"],
                    "target_type": 1,
                    "target_id": p["id"],
                    "is_read": rng.choice([0, 1]),
                    "create_time": r["create_time"] + timedelta(hours=2),
                })
            else:
                c = r["_comment"]
                rows.append({
                    "user_id": c["_author"]["id"],
                    "type": 1,
                    "content": "你的评论《%s》因违反社区规定已被删除" % c["content"][:20],
                    "target_type": 2,
                    "target_id": c["id"],
                    "is_read": rng.choice([0, 1]),
                    "create_time": r["create_time"] + timedelta(hours=2),
                })
        elif r["status"] == 2:
            rows.append({
                "user_id": r["user_id"],
                "type": 2,
                "content": "我们还在持续对您举报的对象进行核查分析中，会进一步收集并审核更大范围的数据与用户行为，请关注后续安全邮件通知来获取结果反馈。您的举报将帮助我们持续优化对这类违规行为的检测方案，感谢您的举报，祝您生活愉快【G.T.I.Security】",
                "target_type": r["report_type"],
                "target_id": r["target_id"],
                "is_read": rng.choice([0, 1]),
                "create_time": r["create_time"] + timedelta(hours=3),
            })

    # 未被举报但被管理员直接下架的内容，也会发通知
    reported_ids = {r["target_id"] for r in reports if r["report_type"] == 1}
    for p in posts:
        if p["is_banned"] and p["id"] not in reported_ids:
            rows.append({
                "user_id": p["author"]["id"],
                "type": 1,
                "content": "你的帖子《%s》因违反社区规定已被下架" % p["title"],
                "target_type": 1,
                "target_id": p["id"],
                "is_read": rng.choice([0, 1]),
                "create_time": rand_time(rng, p["create_time"] + timedelta(hours=6),
                                         NOW - timedelta(minutes=1)),
            })

    rows.sort(key=lambda x: x["create_time"])
    for idx, n in enumerate(rows):
        n["id"] = idx + 1
        n["update_time"] = n["create_time"]
    return rows


def compute_user_counts(users, posts):
    for u in users:
        u["total_like_count"] = sum(p["like_count"] for p in posts
                                    if p["author"]["id"] == u["id"] and p["is_deleted"] == "0")
        if u["follow_count"] is None:
            u["follow_count"] = 0
        if u["fan_count"] is None:
            u["fan_count"] = 0


# ----------------------------------------------------------------------------
# 主流程
# ----------------------------------------------------------------------------
def clean_previous_images(manifest_path):
    if not os.path.exists(manifest_path):
        return
    removed = 0
    with open(manifest_path, "r", encoding="utf-8") as f:
        for line in f:
            rel = line.strip()
            if not rel:
                continue
            p = os.path.join(ROOT, rel.replace("/", os.sep))
            if os.path.exists(p):
                try:
                    os.remove(p)
                    removed += 1
                except OSError:
                    pass
    print("已清理上次生成的图片 %d 张" % removed)


def generate():
    rng = random.Random(SEED)
    global RNG
    RNG = random.Random(SEED + 7)

    os.makedirs(UPLOAD_NEWS, exist_ok=True)
    os.makedirs(UPLOAD_AVATAR, exist_ok=True)
    os.makedirs(UPLOAD_COVER, exist_ok=True)

    clean_previous_images(MANIFEST)

    pool = load_pool()
    print("图片素材池：%d 张（%s）" % (len(pool), POOL_DIR))

    users = build_users(rng, pool)
    print("用户：%d（id %d~%d）" % (len(users), USER_ID_START, USER_ID_START + len(users) - 1))

    # 头像 / 封面文件
    manifest = []
    for u in users:
        make_avatar(u["username"], os.path.join(UPLOAD_AVATAR, u["avatar_file"]), rng)
        manifest.append("uploads/avatar/" + u["avatar_file"])
        if u.get("cover_file"):
            make_photo_variant(u["cover_base"], os.path.join(UPLOAD_COVER, u["cover_file"]),
                               1200, 480, rng)
            manifest.append("uploads/cover/" + u["cover_file"])

    posts = build_posts(rng, users)
    print("帖子：%d（图文 %d / 纯文 %d）" % (
        len(posts), sum(1 for p in posts if p["media_type"] == 2),
        sum(1 for p in posts if p["media_type"] == 1)))

    images = build_images(rng, posts, pool, set(), manifest)
    print("帖子图片：%d" % len(images))

    likes = build_likes(rng, posts, users)
    print("帖子点赞：%d" % len(likes))

    # 评论：先生成对象树，再按时间分配 id，最后回填 parent_id / root_comment_id
    comments = build_comments(rng, posts, users)
    print("评论：%d（其中楼中楼 %d）" % (len(comments), sum(1 for c in comments if c["depth"] > 0)))

    comment_likes = build_comment_likes(rng, comments)
    print("评论点赞：%d" % len(comment_likes))

    dislikes = build_dislikes(rng, comments)
    print("评论踩：%d" % len(dislikes))

    follows = build_follows(rng, users)
    print("关注关系：%d" % len(follows))

    collects = build_collects(rng, posts, users)
    print("收藏：%d" % len(collects))

    disliked_posts = build_disliked_posts(rng, posts, users)
    print("不感兴趣：%d" % len(disliked_posts))

    reports = build_reports(rng, posts, comments, users)
    print("举报：%d" % len(reports))

    notifications = build_notifications(rng, reports, posts)
    print("通知：%d" % len(notifications))

    # 冗余计数回算
    for p in posts:
        p["like_count"] = len([1 for lk in likes if lk["news_id"] == p["id"]])
        p["comment_count"] = len([1 for c in p["comments"] if c["status"] == 1])
        p["collect_count"] = len([1 for r in collects if r["collect_type"] == 1 and r["target_id"] == p["id"]])
        p["view_count"] = (p["like_count"] * rng.randint(5, 12) + rng.randint(20, 300))
        p["status"] = 1 if p["is_banned"] else 0
        p["update_time"] = (p["create_time"] + timedelta(minutes=rng.randint(1, 90))
                            if rng.random() < 0.35 else None)
    for u in users:
        u["total_like_count"] = sum(p["like_count"] for p in posts
                                    if p["author"]["id"] == u["id"] and p["is_deleted"] == "0")
    for c in comments:
        c["like_count"] = len([1 for lk in comment_likes if lk["comment_id"] == c["id"]])
        c["dislike"] = len([1 for d in dislikes if d["comment_id"] == c["id"]])

    # ---------------- 写 SQL ----------------
    w = SqlWriter()
    w.lines.append("-- ===== 数据概览 =====")
    w.lines.append("-- 用户 %d / 帖子 %d / 图片 %d / 评论 %d / 点赞 %d / 关注 %d / 收藏 %d / 举报 %d / 通知 %d"
                   % (len(users), len(posts), len(images), len(comments), len(likes),
                      len(follows), len(collects), len(reports), len(notifications)))
    w.lines.append("")

    w.delete_block(
        ["news_like", "comment_like", "comment_dislike", "user_follow", "user_collect",
         "user_dislike_post", "user_notification", "report", "comment", "news_image", "news"],
        user_ids=(USER_ID_START, USER_ID_START + len(users) - 1),
    )

    w.insert_batch("user",
                   ["id", "username", "password", "role", "create_time", "user_account",
                    "phone", "avatar", "cover", "bio", "birthday", "location",
                    "total_like_count", "follow_count", "fan_count", "status",
                    "is_deleted", "update_time"],
                   [(u["id"], u["username"], u["password"], u["role"], fmt_dt(u["create_time"]),
                     u["user_account"], u["phone"], u["avatar"], u["cover"], u["bio"],
                     u["birthday"], u["location"], u["total_like_count"], u["follow_count"],
                     u["fan_count"], u["status"], u["is_deleted"], fmt_dt(u["create_time"]))
                    for u in users])

    w.insert_batch("news",
                   ["id", "title", "content", "user_id", "create_time", "view_count",
                    "like_count", "media_type", "comment_count", "collect_count",
                    "status", "is_deleted", "update_time"],
                   [(p["id"], p["title"], p["content"], p["author"]["id"], fmt_dt(p["create_time"]),
                     p["view_count"], p["like_count"], p["media_type"], p["comment_count"],
                     p["collect_count"], p["status"], p["is_deleted"], fmt_dt(p["update_time"]))
                    for p in posts])

    w.insert_batch("news_image",
                   ["id", "news_id", "image_url", "sort_order", "create_time", "media_type",
                    "cover_url", "width", "height", "duration", "is_delete"],
                   [(im["id"], im["news_id"], im["image_url"], im["sort_order"],
                     fmt_dt(im["create_time"]), im["media_type"], im["cover_url"],
                     im["width"], im["height"], None, im["is_delete"])
                    for im in images])

    w.insert_batch("comment",
                   ["id", "news_id", "user_id", "parent_id", "to_user_id", "content",
                    "status", "create_time", "update_time", "root_comment_id",
                    "like_count", "dislike"],
                   [(c["id"], c["news_id"], c["user_id"], c["parent_id"] or 0, c["to_user_id"],
                     c["content"], c["status"], fmt_dt(c["create_time"]), fmt_dt(c["create_time"]),
                     c["root_id"], c["like_count"], c["dislike"])
                    for c in comments])

    w.insert_batch("news_like",
                   ["id", "news_id", "user_id", "is_cancel", "create_time", "update_time", "is_deleted"],
                   [(lk["id"], lk["news_id"], lk["user_id"], 0, fmt_dt(lk["create_time"]),
                     fmt_dt(lk["update_time"]), 0) for lk in likes])

    w.insert_batch("comment_like",
                   ["id", "comment_id", "user_id", "is_cancel", "create_time", "update_time", "is_deleted"],
                   [(lk["id"], lk["comment_id"], lk["user_id"], 0, fmt_dt(lk["create_time"]),
                     fmt_dt(lk["update_time"]), 0) for lk in comment_likes])

    w.insert_batch("comment_dislike",
                   ["id", "comment_id", "user_id", "is_cancel", "create_time", "update_time", "is_deleted"],
                   [(d["id"], d["comment_id"], d["user_id"], 0, fmt_dt(d["create_time"]),
                     fmt_dt(d["update_time"]), 0) for d in dislikes])

    w.insert_batch("user_follow",
                   ["id", "user_id", "follow_user_id", "is_cancel", "create_time", "update_time", "is_deleted"],
                   [(f["id"], f["user_id"], f["follow_user_id"], 0, fmt_dt(f["create_time"]),
                     fmt_dt(f["update_time"]), 0) for f in follows])

    w.insert_batch("user_collect",
                   ["id", "user_id", "collect_type", "target_id", "is_cancel", "create_time", "update_time", "is_deleted"],
                   [(c["id"], c["user_id"], c["collect_type"], c["target_id"], 0,
                     fmt_dt(c["create_time"]), fmt_dt(c["update_time"]), 0) for c in collects])

    w.insert_batch("user_dislike_post",
                   ["id", "post_id", "user_id", "is_cancel", "create_time", "update_time", "is_deleted"],
                   [(d["id"], d["post_id"], d["user_id"], 0, fmt_dt(d["create_time"]),
                     fmt_dt(d["update_time"]), 0) for d in disliked_posts])

    w.insert_batch("report",
                   ["id", "report_type", "target_id", "user_id", "reason_type", "remark",
                    "status", "create_time", "update_time", "is_deleted"],
                   [(r["id"], r["report_type"], r["target_id"], r["user_id"], r["reason_type"],
                     r["remark"], r["status"], fmt_dt(r["create_time"]),
                     fmt_dt(r["update_time"]), 0) for r in reports])

    w.insert_batch("user_notification",
                   ["id", "user_id", "type", "content", "target_type", "target_id",
                    "is_read", "create_time", "update_time", "is_deleted"],
                   [(n["id"], n["user_id"], n["type"], n["content"], n["target_type"],
                     n["target_id"], n["is_read"], fmt_dt(n["create_time"]),
                     fmt_dt(n["update_time"]), 0) for n in notifications])

    w.save(SQL_PATH)
    with open(MANIFEST, "w", encoding="utf-8") as f:
        f.write("\n".join(manifest))

    size_mb = os.path.getsize(SQL_PATH) / 1024 / 1024
    print("SQL 已生成：%s（%.2f MB，%d 行）" % (SQL_PATH, size_mb, w.row_total))
    print("图片：%d 个文件（帖子图 %d / 头像 %d / 封面 %d）"
          % (len(manifest), len(images), len(users),
             sum(1 for u in users if u.get("cover_file"))))
    return {
        "users": len(users), "posts": len(posts), "images": len(images),
        "comments": len(comments), "likes": len(likes), "follows": len(follows),
        "collects": len(collects), "reports": len(reports), "notifications": len(notifications),
    }


def mysql_cmd(extra):
    base = [MYSQL_BIN,
            "--user=%s" % DB["user"],
            "--password=%s" % DB["password"],
            "--host=%s" % DB["host"],
            "--port=%d" % DB["port"],
            "--default-character-set=utf8mb4"]
    return base + extra


def apply_sql():
    if not os.path.exists(SQL_PATH):
        print("seed.sql 不存在，请先运行 python seed.py")
        sys.exit(1)
    with open(SQL_PATH, "rb") as f:
        proc = subprocess.run(mysql_cmd([DB["name"]]), stdin=f,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if proc.returncode != 0:
        print("导入失败：")
        print(proc.stderr.decode("utf-8", "ignore"))
        sys.exit(1)
    print("已导入 %s" % SQL_PATH)


def query(sql):
    proc = subprocess.run(mysql_cmd(["-N", "-B", "--execute", sql, DB["name"]]),
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if proc.returncode != 0:
        print(proc.stderr.decode("utf-8", "ignore"))
        return []
    out = proc.stdout.decode("utf-8", "ignore").strip()
    if not out:
        return []
    return [line.split("\t") for line in out.splitlines()]


def verify():
    print("\n================ 数据体量 ================")
    tables = ["user", "news", "news_image", "comment", "news_like", "comment_like",
              "comment_dislike", "user_follow", "user_collect", "user_dislike_post",
              "report", "user_notification"]
    for t in tables:
        rows = query("SELECT COUNT(*) FROM `%s`" % t)
        print("%-20s %s" % (t, rows[0][0] if rows else "?"))

    print("\n================ 一致性对账 ================")
    checks = [
        ("帖子点赞数 != 点赞记录数",
         "SELECT COUNT(*) FROM news n WHERE n.like_count <> "
         "(SELECT COUNT(*) FROM news_like l WHERE l.news_id=n.id AND l.is_cancel=0 AND l.is_deleted=0)"),
        ("帖子评论数 != 有效评论数(含楼中楼)",
         "SELECT COUNT(*) FROM news n WHERE n.comment_count <> "
         "(SELECT COUNT(*) FROM comment c WHERE c.news_id=n.id AND c.status=1)"),
        ("帖子收藏数 != 收藏记录数",
         "SELECT COUNT(*) FROM news n WHERE n.collect_count <> "
         "(SELECT COUNT(*) FROM user_collect uc WHERE uc.collect_type=1 AND uc.target_id=n.id "
         "AND uc.is_cancel=0 AND uc.is_deleted=0)"),
        ("评论点赞数 != 点赞记录数",
         "SELECT COUNT(*) FROM comment c WHERE c.status=1 AND c.like_count <> "
         "(SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id=c.id AND cl.is_cancel=0 AND cl.is_deleted=0)"),
        ("评论踩数 != 踩记录数",
         "SELECT COUNT(*) FROM comment c WHERE c.dislike <> "
         "(SELECT COUNT(*) FROM comment_dislike cd WHERE cd.comment_id=c.id AND cd.is_cancel=0 AND cd.is_deleted=0)"),
        ("用户获赞总数 != 其帖子点赞之和",
         "SELECT COUNT(*) FROM `user` u WHERE u.id>=16 AND u.total_like_count <> "
         "(SELECT COALESCE(SUM(n.like_count),0) FROM news n WHERE n.user_id=u.id AND n.is_deleted='0')"),
        ("用户关注数 != 关注记录数",
         "SELECT COUNT(*) FROM `user` u WHERE u.id>=16 AND u.follow_count <> "
         "(SELECT COUNT(*) FROM user_follow f WHERE f.user_id=u.id AND f.is_cancel=0 AND f.is_deleted=0)"),
        ("用户粉丝数 != 被关注记录数",
         "SELECT COUNT(*) FROM `user` u WHERE u.id>=16 AND u.fan_count <> "
         "(SELECT COUNT(*) FROM user_follow f WHERE f.follow_user_id=u.id AND f.is_cancel=0 AND f.is_deleted=0)"),
        ("评论引用了不存在的帖子",
         "SELECT COUNT(*) FROM comment c LEFT JOIN news n ON n.id=c.news_id WHERE n.id IS NULL"),
        ("点赞引用了不存在的帖子",
         "SELECT COUNT(*) FROM news_like l LEFT JOIN news n ON n.id=l.news_id WHERE n.id IS NULL"),
        ("举报已通过但目标未下架(帖子)",
         "SELECT COUNT(*) FROM report r JOIN news n ON n.id=r.target_id "
         "WHERE r.report_type=1 AND r.status=1 AND n.status<>1"),
        ("举报已通过但目标未删除(评论)",
         "SELECT COUNT(*) FROM report r JOIN comment c ON c.id=r.target_id "
         "WHERE r.report_type=2 AND r.status=1 AND c.status<>0"),
        ("楼中楼 parent_id 指向不存在评论",
         "SELECT COUNT(*) FROM comment c LEFT JOIN comment p ON p.id=c.parent_id "
         "WHERE c.parent_id<>0 AND p.id IS NULL"),
        ("楼中楼 root_comment_id 为空",
         "SELECT COUNT(*) FROM comment WHERE parent_id<>0 AND root_comment_id IS NULL"),
        ("图文帖缺图片",
         "SELECT COUNT(*) FROM news n WHERE n.media_type=2 AND n.is_deleted='0' AND NOT EXISTS "
         "(SELECT 1 FROM news_image ni WHERE ni.news_id=n.id AND ni.is_delete=0)"),
        ("纯文帖带图片",
         "SELECT COUNT(*) FROM news n WHERE n.media_type=1 AND EXISTS "
         "(SELECT 1 FROM news_image ni WHERE ni.news_id=n.id AND ni.is_delete=0)"),
        ("用户名重复", "SELECT COUNT(*) FROM (SELECT username FROM `user` GROUP BY username HAVING COUNT(*)>1) t"),
        ("帖子标题重复(strict)",
         "SELECT COUNT(*) FROM (SELECT title FROM news GROUP BY title HAVING COUNT(*)>1) t"),
        ("帖子 id 不连续",
         "SELECT CASE WHEN MAX(id)=COUNT(*) AND MIN(id)=1 THEN 'OK' ELSE CONCAT('bad: max=',MAX(id),' cnt=',COUNT(*)) END FROM news"),
        ("评论 id 不连续",
         "SELECT CASE WHEN MAX(id)=COUNT(*) AND MIN(id)=1 THEN 'OK' ELSE CONCAT('bad: max=',MAX(id),' cnt=',COUNT(*)) END FROM comment"),
    ]
    for name, sql in checks:
        rows = query(sql)
        val = rows[0][0] if rows else "?"
        flag = "OK" if val in ("0", "OK") else "!!"
        print("[%s] %-32s %s" % (flag, name, val))

    print("\n================ 数值分布抽样 ================")
    print("帖子点赞分档：")
    rows = query("SELECT CASE WHEN like_count=0 THEN '0' WHEN like_count<=5 THEN '1-5' "
                 "WHEN like_count<=10 THEN '6-10' WHEN like_count<=30 THEN '11-30' "
                 "ELSE '30+' END g, COUNT(*) FROM news GROUP BY g ORDER BY MIN(like_count)")
    for r in rows:
        print("  %-6s %s" % (r[0], r[1]))
    print("帖子评论分档：")
    rows = query("SELECT CASE WHEN comment_count=0 THEN '0' WHEN comment_count<=3 THEN '1-3' "
                 "WHEN comment_count<=10 THEN '4-10' ELSE '10+' END g, COUNT(*) FROM news "
                 "GROUP BY g ORDER BY MIN(comment_count)")
    for r in rows:
        print("  %-6s %s" % (r[0], r[1]))
    print("帖子状态：")
    rows = query("SELECT status, COUNT(*) FROM news GROUP BY status")
    for r in rows:
        print("  status=%s %s" % (r[0], r[1]))
    print("时间跨度：")
    rows = query("SELECT MIN(create_time), MAX(create_time) FROM news")
    if rows:
        print("  发帖 %s ~ %s" % (rows[0][0], rows[0][1]))
    rows = query("SELECT MIN(create_time), MAX(create_time) FROM `user` WHERE id>=16")
    if rows:
        print("  注册 %s ~ %s" % (rows[0][0], rows[0][1]))


def flush_redis():
    if not os.path.exists(REDIS_CLI):
        print("未找到 redis-cli：%s" % REDIS_CLI)
        return
    proc = subprocess.run([REDIS_CLI, "-p", "6379", "FLUSHALL"],
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    print("redis flush:", proc.stdout.decode("utf-8", "ignore").strip() or proc.stderr.decode("utf-8", "ignore").strip())


def main():
    ap = argparse.ArgumentParser(description="社区库数据初始化生成器")
    ap.add_argument("--apply", action="store_true", help="生成后导入数据库")
    ap.add_argument("--verify", action="store_true", help="只做对账校验")
    ap.add_argument("--flush-redis", action="store_true", help="导入后清空 Redis")
    ap.add_argument("--only-verify", action="store_true", help=argparse.SUPPRESS)
    args = ap.parse_args()

    if args.verify and not args.apply:
        verify()
        return

    stats = generate()
    print("\n生成结果：%s" % stats)

    if args.apply:
        apply_sql()
        if args.flush_redis:
            flush_redis()
        verify()


if __name__ == "__main__":
    main()
