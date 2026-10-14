/**
 * Mock 静态数据：店铺、商品（多规格 SKU）、社区帖子、用户
 * 价格单位：元
 */

export const shops = [
  { id: 'S001', name: '优选数码旗舰店', logo: '', rating: 4.8 },
  { id: 'S002', name: '轻食生活馆', logo: '', rating: 4.6 },
  { id: 'S003', name: '山野户外专营店', logo: '', rating: 4.9 }
]

/**
 * 商品规格模型：
 * specs: [{ name: '颜色', values: ['黑色','白色'] }, { name: '尺寸', values: ['S','M','L'] }]
 * skus: [{ id, specValues: {颜色:'黑色', 尺寸:'M'}, price, stock }]
 * stock=0 或商品 status='off' 表示失效
 */
export const goodsList = [
  {
    id: 'G001',
    shopId: 'S001',
    title: '无线降噪蓝牙耳机 Pro 2 长续航运动跑步',
    cover: 'https://picsum.photos/seed/g001/400/400',
    images: [
      'https://picsum.photos/seed/g001/750/750',
      'https://picsum.photos/seed/g001b/750/750'
    ],
    price: 299,
    sales: 1200,
    status: 'on',
    specs: [
      { name: '颜色', values: ['曜石黑', '珍珠白'] },
      { name: '型号', values: ['标准版', '降噪版'] }
    ],
    skus: [
      { id: 'G001-A', specValues: { 颜色: '曜石黑', 型号: '标准版' }, price: 299, stock: 50 },
      { id: 'G001-B', specValues: { 颜色: '曜石黑', 型号: '降噪版' }, price: 399, stock: 20 },
      { id: 'G001-C', specValues: { 颜色: '珍珠白', 型号: '标准版' }, price: 299, stock: 8 },
      { id: 'G001-D', specValues: { 颜色: '珍珠白', 型号: '降噪版' }, price: 399, stock: 0 }
    ]
  },
  {
    id: 'G002',
    shopId: 'S001',
    title: '智能手表 心率血氧监测 防水运动手环',
    cover: 'https://picsum.photos/seed/g002/400/400',
    images: ['https://picsum.photos/seed/g002/750/750'],
    price: 599,
    sales: 860,
    status: 'on',
    specs: [
      { name: '颜色', values: ['午夜黑', '星光银'] },
      { name: '表带', values: ['硅胶', '金属'] }
    ],
    skus: [
      { id: 'G002-A', specValues: { 颜色: '午夜黑', 表带: '硅胶' }, price: 599, stock: 30 },
      { id: 'G002-B', specValues: { 颜色: '午夜黑', 表带: '金属' }, price: 699, stock: 12 },
      { id: 'G002-C', specValues: { 颜色: '星光银', 表带: '硅胶' }, price: 599, stock: 15 },
      { id: 'G002-D', specValues: { 颜色: '星光银', 表带: '金属' }, price: 699, stock: 3 }
    ]
  },
  {
    id: 'G003',
    shopId: 'S002',
    title: '每日坚果混合装 30包 孕妇儿童零食大礼包',
    cover: 'https://picsum.photos/seed/g003/400/400',
    images: ['https://picsum.photos/seed/g003/750/750'],
    price: 89,
    sales: 5600,
    status: 'on',
    specs: [{ name: '规格', values: ['7袋体验装', '15袋中盒', '30袋大箱'] }],
    skus: [
      { id: 'G003-A', specValues: { 规格: '7袋体验装' }, price: 29, stock: 200 },
      { id: 'G003-B', specValues: { 规格: '15袋中盒' }, price: 59, stock: 120 },
      { id: 'G003-C', specValues: { 规格: '30袋大箱' }, price: 89, stock: 60 }
    ]
  },
  {
    id: 'G004',
    shopId: 'S002',
    title: '冷萃咖啡液 0糖0脂 美式浓缩 12杯装',
    cover: 'https://picsum.photos/seed/g004/400/400',
    images: ['https://picsum.photos/seed/g004/750/750'],
    price: 68,
    sales: 2300,
    status: 'off', // 已下架 -> 购物车失效商品
    specs: [{ name: '口味', values: ['原味', '榛果'] }],
    skus: [
      { id: 'G004-A', specValues: { 口味: '原味' }, price: 68, stock: 40 },
      { id: 'G004-B', specValues: { 口味: '榛果' }, price: 72, stock: 25 }
    ]
  },
  {
    id: 'G005',
    shopId: 'S003',
    title: '户外冲锋衣 三合一防水防风 登山徒步外套',
    cover: 'https://picsum.photos/seed/g005/400/400',
    images: ['https://picsum.photos/seed/g005/750/750'],
    price: 459,
    sales: 430,
    status: 'on',
    specs: [
      { name: '颜色', values: ['军绿', '藏青', '烈焰橙'] },
      { name: '尺码', values: ['M', 'L', 'XL', 'XXL'] }
    ],
    skus: [
      { id: 'G005-A', specValues: { 颜色: '军绿', 尺码: 'M' }, price: 459, stock: 10 },
      { id: 'G005-B', specValues: { 颜色: '军绿', 尺码: 'L' }, price: 459, stock: 6 },
      { id: 'G005-C', specValues: { 颜色: '军绿', 尺码: 'XL' }, price: 459, stock: 0 },
      { id: 'G005-D', specValues: { 颜色: '军绿', 尺码: 'XXL' }, price: 489, stock: 2 },
      { id: 'G005-E', specValues: { 颜色: '藏青', 尺码: 'M' }, price: 459, stock: 8 },
      { id: 'G005-F', specValues: { 颜色: '藏青', 尺码: 'L' }, price: 459, stock: 4 },
      { id: 'G005-G', specValues: { 颜色: '藏青', 尺码: 'XL' }, price: 459, stock: 9 },
      { id: 'G005-H', specValues: { 颜色: '藏青', 尺码: 'XXL' }, price: 489, stock: 0 },
      { id: 'G005-I', specValues: { 颜色: '烈焰橙', 尺码: 'M' }, price: 499, stock: 3 },
      { id: 'G005-J', specValues: { 颜色: '烈焰橙', 尺码: 'L' }, price: 499, stock: 5 },
      { id: 'G005-K', specValues: { 颜色: '烈焰橙', 尺码: 'XL' }, price: 499, stock: 1 },
      { id: 'G005-L', specValues: { 颜色: '烈焰橙', 尺码: 'XXL' }, price: 529, stock: 0 }
    ]
  },
  {
    id: 'G006',
    shopId: 'S003',
    title: '轻量化露营帐篷 双人防暴雨 速开野外装备',
    cover: 'https://picsum.photos/seed/g006/400/400',
    images: ['https://picsum.photos/seed/g006/750/750'],
    price: 328,
    sales: 190,
    status: 'on',
    specs: [{ name: '款式', values: ['双人款', '四人款'] }],
    skus: [
      { id: 'G006-A', specValues: { 款式: '双人款' }, price: 328, stock: 18 },
      { id: 'G006-B', specValues: { 款式: '四人款' }, price: 468, stock: 5 }
    ]
  }
]

export const mockUser = {
  id: 'U001',
  nickname: '山间来客',
  avatar: 'https://picsum.photos/seed/u001/200/200',
  phone: '138****8888'
}

export const mockPosts = [
  {
    id: 'P001',
    author: { nickname: '露营狂人', avatar: 'https://picsum.photos/seed/a1/100/100' },
    title: '周末露营装备清单分享',
    content: '这次带上了新买的帐篷和炉具，山顶日出太美了！附上清单：帐篷、防潮垫、炉头、气罐、折叠椅……',
    images: Array.from({ length: 4 }, (_, i) => `https://picsum.photos/seed/p00${i}/600/800`),
    likes: 328,
    comments: 45,
    createTime: Date.now() - 2 * 3600 * 1000,
    liked: false
  },
  {
    id: 'P002',
    author: { nickname: '数码小白', avatar: 'https://picsum.photos/seed/a2/100/100' },
    title: '降噪耳机横评：千元内谁最强',
    content: '实测了 5 款热门降噪耳机，从佩戴舒适度到降噪深度逐项打分，结论放在最后，性价比之王居然是它……',
    images: Array.from({ length: 3 }, (_, i) => `https://picsum.photos/seed/p01${i}/600/600`),
    likes: 512,
    comments: 89,
    createTime: Date.now() - 5 * 3600 * 1000,
    liked: false
  },
  {
    id: 'P003',
    author: { nickname: '轻食日记', avatar: 'https://picsum.photos/seed/a3/100/100' },
    title: '一周减脂餐打卡 Day3',
    content: '今天的午餐是鸡胸肉沙拉配冷萃咖啡，0糖0脂无负担，坚持第 3 天，已经掉了 1.2kg！',
    images: ['https://picsum.photos/seed/p020/600/900'],
    likes: 189,
    comments: 23,
    createTime: Date.now() - 8 * 3600 * 1000,
    liked: false
  },
  {
    id: 'P004',
    author: { nickname: '徒步老王', avatar: 'https://picsum.photos/seed/a4/100/100' },
    title: '冲锋衣到底要不要买三合一？',
    content: '穿了十年的冲锋衣，从单层到三合一都试过。结论：南方单穿内胆，北方必须三合一，听我的没错。',
    images: Array.from({ length: 6 }, (_, i) => `https://picsum.photos/seed/p03${i}/600/750`),
    likes: 764,
    comments: 132,
    createTime: Date.now() - 26 * 3600 * 1000,
    liked: false
  },
  {
    id: 'P005',
    author: { nickname: '手冲咖啡君', avatar: 'https://picsum.photos/seed/a5/100/100' },
    title: '冷萃 vs 手冲，口感对比',
    content: '同一支豆子，两种做法，风味差异比想象的大。冷萃更甜润，手冲香气更立体，你们站哪边？',
    images: Array.from({ length: 2 }, (_, i) => `https://picsum.photos/seed/p04${i}/600/600`),
    likes: 233,
    comments: 56,
    createTime: Date.now() - 30 * 3600 * 1000,
    liked: false
  },
  {
    id: 'P006',
    author: { nickname: '数码小白', avatar: 'https://picsum.photos/seed/a2/100/100' },
    title: '智能手表一个月真实体验',
    content: '心率、血氧、睡眠监测都测了，准确度对得起价格，就是续航比官方宣传少了两天……',
    images: Array.from({ length: 5 }, (_, i) => `https://picsum.photos/seed/p05${i}/600/800`),
    likes: 421,
    comments: 67,
    createTime: Date.now() - 48 * 3600 * 1000,
    liked: false
  }
]
