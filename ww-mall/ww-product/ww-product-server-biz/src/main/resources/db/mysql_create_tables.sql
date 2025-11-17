-- MySQL 8 建表脚本
-- 数据库: product

-- ==========================================
-- 1. 商品品牌表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '品牌名称',
    `img` VARCHAR(500) DEFAULT NULL COMMENT '品牌图片',
    `sort` INT DEFAULT 0 COMMENT '品牌排序',
    `description` TEXT DEFAULT NULL COMMENT '品牌描述',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态 [0: 禁用  1：启用]',
    `version` BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除 [0: 未删除  1: 已删除]',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品品牌表';

-- ==========================================
-- 2. 商品分类表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类id（根分类为0）',
    `status` TINYINT(1) DEFAULT 1 COMMENT '是否显示[0-不显示，1显示]',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `icon` VARCHAR(500) DEFAULT NULL COMMENT '图标地址',
    `version` BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除 [0: 未删除  1: 已删除]',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ==========================================
-- 3. 商品属性表（SKU属性名）
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_property` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '属性名称',
    `version` BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除 [0: 未删除  1: 已删除]',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品属性表（SKU属性名）';

-- ==========================================
-- 4. 商品属性值表（SKU属性值）
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_property_value` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `property_id` BIGINT NOT NULL COMMENT '属性项的编号（关联product_property.id）',
    `name` VARCHAR(255) NOT NULL COMMENT 'SKU属性值',
    `version` BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除 [0: 未删除  1: 已删除]',
    PRIMARY KEY (`id`),
    KEY `idx_property_id` (`property_id`),
    KEY `idx_name` (`name`),
    CONSTRAINT `fk_property_value_property` FOREIGN KEY (`property_id`) REFERENCES `product_property` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品属性值表（SKU属性值）';

-- ==========================================
-- 5. 商品SPU表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_spu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '商品名称',
    `keyword` VARCHAR(255) DEFAULT NULL COMMENT '关键字',
    `spu_type` VARCHAR(50) DEFAULT NULL COMMENT '商品类型【虚拟、实物】（枚举：VIRTUAL, PHYSICAL）',
    `category_id` BIGINT DEFAULT NULL COMMENT '商品分类编号（关联product_category.id）',
    `brand_id` BIGINT DEFAULT NULL COMMENT '商品品牌编号（关联product_brand.id）',
    `img` VARCHAR(500) DEFAULT NULL COMMENT '商品封面图',
    `slider_img_list` JSON DEFAULT NULL COMMENT '商品轮播图（JSON数组）',
    `introduction` VARCHAR(500) DEFAULT NULL COMMENT '商品简介',
    `description` TEXT DEFAULT NULL COMMENT '商品详情',
    `status` VARCHAR(50) DEFAULT NULL COMMENT '商品状态（枚举：DOWN-下架, UP-上架, FREEZE-冻结）',
    `spec_type` TINYINT(1) DEFAULT 0 COMMENT '规格类型 [0-单规格, 1-多规格]',
    `delivery_template_id` BIGINT DEFAULT 1 COMMENT '运费模板【默认：1 包邮】',
    `sales_count` INT DEFAULT 0 COMMENT '商品销量',
    `browse_count` INT DEFAULT 0 COMMENT '浏览量',
    `min_price` BIGINT DEFAULT 0 COMMENT '最低sku价格（单位：分）',
    `version` BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除 [0: 未删除  1: 已删除]',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_keyword` (`keyword`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_brand_id` (`brand_id`),
    CONSTRAINT `fk_spu_category` FOREIGN KEY (`category_id`) REFERENCES `product_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_spu_brand` FOREIGN KEY (`brand_id`) REFERENCES `product_brand` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- ==========================================
-- 6. 商品SKU表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_sku` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `spu_id` BIGINT NOT NULL COMMENT 'SPU编号（关联product_spu.id）',
    `properties` JSON DEFAULT NULL COMMENT '属性数组（JSON格式）',
    `price` BIGINT DEFAULT 0 COMMENT '商品价格（单位：分）',
    `market_price` BIGINT DEFAULT 0 COMMENT '市场价（单位：分）',
    `cost_price` BIGINT DEFAULT 0 COMMENT '成本价（单位：分）',
    `img` VARCHAR(500) DEFAULT NULL COMMENT '图片地址',
    `bar_code` VARCHAR(100) DEFAULT NULL COMMENT '条形码',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `version` BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建者ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除 [0: 未删除  1: 已删除]',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    CONSTRAINT `fk_sku_spu` FOREIGN KEY (`spu_id`) REFERENCES `product_spu` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- ==========================================
-- 初始化数据
-- ==========================================

-- ==========================================
-- 1. 商品品牌表初始化数据
-- ==========================================
INSERT INTO `product_brand` (`name`, `img`, `sort`, `description`, `status`, `version`, `creator_id`, `updater_id`) VALUES
('苹果', 'https://example.com/brand/apple.png', 1, 'Apple Inc. 是全球知名的科技公司，以创新设计和优质产品著称', 1, 0, 1, 1),
('华为', 'https://example.com/brand/huawei.png', 2, '华为技术有限公司，全球领先的信息与通信技术解决方案供应商', 1, 0, 1, 1),
('小米', 'https://example.com/brand/xiaomi.png', 3, '小米科技有限责任公司，以高性价比的智能硬件产品闻名', 1, 0, 1, 1),
('OPPO', 'https://example.com/brand/oppo.png', 4, 'OPPO 是一家专注于智能终端产品、软件和互联网服务的科技公司', 1, 0, 1, 1),
('vivo', 'https://example.com/brand/vivo.png', 5, 'vivo 是一家以设计驱动创造伟大产品，打造以智能终端和智慧服务为核心的科技公司', 1, 0, 1, 1),
('耐克', 'https://example.com/brand/nike.png', 6, 'Nike 是全球著名的体育运动品牌，以"Just Do It"为品牌口号', 1, 0, 1, 1),
('阿迪达斯', 'https://example.com/brand/adidas.png', 7, 'Adidas 是德国运动用品制造商，世界三大运动品牌之一', 1, 0, 1, 1),
('李宁', 'https://example.com/brand/li-ning.png', 8, '李宁公司是中国领先的体育品牌企业之一，以"一切皆有可能"为品牌口号', 1, 0, 1, 1),
('联想', 'https://example.com/brand/lenovo.png', 9, '联想集团是全球知名的科技公司，主要生产个人电脑、服务器、智能手机等', 1, 0, 1, 1),
('戴尔', 'https://example.com/brand/dell.png', 10, 'Dell 是全球知名的计算机技术公司，提供企业级解决方案和个人电脑产品', 1, 0, 1, 1),
('三星', 'https://example.com/brand/samsung.png', 11, 'Samsung 是韩国最大的跨国企业集团，业务涵盖电子、重工、金融等领域', 1, 0, 1, 1),
('索尼', 'https://example.com/brand/sony.png', 12, 'Sony 是日本一家全球知名的大型综合性跨国企业集团，以电子产品闻名', 1, 0, 1, 1);

-- ==========================================
-- 2. 商品分类表初始化数据（多层级树形结构）
-- ==========================================
-- 一级分类
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('电子产品', 0, 1, 1, 'https://example.com/icon/electronics.png', 0, 1, 1),
('服装鞋帽', 0, 1, 2, 'https://example.com/icon/clothing.png', 0, 1, 1),
('食品饮料', 0, 1, 3, 'https://example.com/icon/food.png', 0, 1, 1),
('美妆个护', 0, 1, 4, 'https://example.com/icon/beauty.png', 0, 1, 1),
('家居用品', 0, 1, 5, 'https://example.com/icon/home.png', 0, 1, 1),
('运动户外', 0, 1, 6, 'https://example.com/icon/sports.png', 0, 1, 1),
('图书文教', 0, 1, 7, 'https://example.com/icon/books.png', 0, 1, 1),
('汽车用品', 0, 1, 8, 'https://example.com/icon/car.png', 0, 1, 1);

-- 二级分类 - 电子产品
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('手机通讯', 1, 1, 1, 'https://example.com/icon/phone.png', 0, 1, 1),
('电脑办公', 1, 1, 2, 'https://example.com/icon/computer.png', 0, 1, 1),
('数码配件', 1, 1, 3, 'https://example.com/icon/accessories.png', 0, 1, 1),
('智能设备', 1, 1, 4, 'https://example.com/icon/smart.png', 0, 1, 1),
('家用电器', 1, 1, 5, 'https://example.com/icon/appliance.png', 0, 1, 1);

-- 三级分类 - 手机通讯
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('智能手机', 9, 1, 1, NULL, 0, 1, 1),
('功能手机', 9, 1, 2, NULL, 0, 1, 1),
('手机配件', 9, 1, 3, NULL, 0, 1, 1),
('手机壳', 9, 1, 4, NULL, 0, 1, 1),
('充电器', 9, 1, 5, NULL, 0, 1, 1),
('数据线', 9, 1, 6, NULL, 0, 1, 1);

-- 三级分类 - 电脑办公
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('笔记本电脑', 10, 1, 1, NULL, 0, 1, 1),
('台式电脑', 10, 1, 2, NULL, 0, 1, 1),
('平板电脑', 10, 1, 3, NULL, 0, 1, 1),
('显示器', 10, 1, 4, NULL, 0, 1, 1),
('键盘鼠标', 10, 1, 5, NULL, 0, 1, 1),
('打印机', 10, 1, 6, NULL, 0, 1, 1),
('办公文具', 10, 1, 7, NULL, 0, 1, 1);

-- 三级分类 - 数码配件
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('耳机音响', 11, 1, 1, NULL, 0, 1, 1),
('存储设备', 11, 1, 2, NULL, 0, 1, 1),
('摄影摄像', 11, 1, 3, NULL, 0, 1, 1),
('智能穿戴', 11, 1, 4, NULL, 0, 1, 1);

-- 三级分类 - 智能设备
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('智能音箱', 12, 1, 1, NULL, 0, 1, 1),
('智能门锁', 12, 1, 2, NULL, 0, 1, 1),
('智能摄像头', 12, 1, 3, NULL, 0, 1, 1),
('智能开关', 12, 1, 4, NULL, 0, 1, 1);

-- 三级分类 - 家用电器
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('大家电', 13, 1, 1, NULL, 0, 1, 1),
('厨房电器', 13, 1, 2, NULL, 0, 1, 1),
('生活电器', 13, 1, 3, NULL, 0, 1, 1),
('个人护理', 13, 1, 4, NULL, 0, 1, 1);

-- 二级分类 - 服装鞋帽
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('男装', 2, 1, 1, 'https://example.com/icon/men.png', 0, 1, 1),
('女装', 2, 1, 2, 'https://example.com/icon/women.png', 0, 1, 1),
('童装', 2, 1, 3, 'https://example.com/icon/kids.png', 0, 1, 1),
('鞋靴', 2, 1, 4, 'https://example.com/icon/shoes.png', 0, 1, 1),
('箱包', 2, 1, 5, 'https://example.com/icon/bag.png', 0, 1, 1),
('配饰', 2, 1, 6, 'https://example.com/icon/accessory.png', 0, 1, 1);

-- 三级分类 - 男装
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('T恤', 14, 1, 1, NULL, 0, 1, 1),
('衬衫', 14, 1, 2, NULL, 0, 1, 1),
('外套', 14, 1, 3, NULL, 0, 1, 1),
('裤子', 14, 1, 4, NULL, 0, 1, 1),
('牛仔裤', 14, 1, 5, NULL, 0, 1, 1),
('休闲裤', 14, 1, 6, NULL, 0, 1, 1),
('运动服', 14, 1, 7, NULL, 0, 1, 1);

-- 三级分类 - 女装
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('连衣裙', 15, 1, 1, NULL, 0, 1, 1),
('上衣', 15, 1, 2, NULL, 0, 1, 1),
('下装', 15, 1, 3, NULL, 0, 1, 1),
('外套', 15, 1, 4, NULL, 0, 1, 1),
('内衣', 15, 1, 5, NULL, 0, 1, 1),
('睡衣', 15, 1, 6, NULL, 0, 1, 1);

-- 三级分类 - 童装
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('男童装', 16, 1, 1, NULL, 0, 1, 1),
('女童装', 16, 1, 2, NULL, 0, 1, 1),
('婴儿装', 16, 1, 3, NULL, 0, 1, 1),
('童鞋', 16, 1, 4, NULL, 0, 1, 1);

-- 三级分类 - 鞋靴
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('运动鞋', 17, 1, 1, NULL, 0, 1, 1),
('休闲鞋', 17, 1, 2, NULL, 0, 1, 1),
('皮鞋', 17, 1, 3, NULL, 0, 1, 1),
('靴子', 17, 1, 4, NULL, 0, 1, 1),
('凉鞋', 17, 1, 5, NULL, 0, 1, 1),
('拖鞋', 17, 1, 6, NULL, 0, 1, 1);

-- 二级分类 - 食品饮料
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('休闲零食', 3, 1, 1, 'https://example.com/icon/snacks.png', 0, 1, 1),
('生鲜食品', 3, 1, 2, 'https://example.com/icon/fresh.png', 0, 1, 1),
('酒水饮料', 3, 1, 3, 'https://example.com/icon/drinks.png', 0, 1, 1),
('茶饮冲调', 3, 1, 4, 'https://example.com/icon/tea.png', 0, 1, 1),
('粮油调味', 3, 1, 5, 'https://example.com/icon/grain.png', 0, 1, 1);

-- 三级分类 - 休闲零食
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('坚果炒货', 20, 1, 1, NULL, 0, 1, 1),
('饼干糕点', 20, 1, 2, NULL, 0, 1, 1),
('膨化食品', 20, 1, 3, NULL, 0, 1, 1),
('糖果巧克力', 20, 1, 4, NULL, 0, 1, 1),
('肉干肉脯', 20, 1, 5, NULL, 0, 1, 1);

-- 三级分类 - 生鲜食品
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('新鲜水果', 21, 1, 1, NULL, 0, 1, 1),
('新鲜蔬菜', 21, 1, 2, NULL, 0, 1, 1),
('海鲜水产', 21, 1, 3, NULL, 0, 1, 1),
('肉禽蛋品', 21, 1, 4, NULL, 0, 1, 1);

-- 三级分类 - 酒水饮料
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('白酒', 22, 1, 1, NULL, 0, 1, 1),
('啤酒', 22, 1, 2, NULL, 0, 1, 1),
('葡萄酒', 22, 1, 3, NULL, 0, 1, 1),
('碳酸饮料', 22, 1, 4, NULL, 0, 1, 1),
('果汁饮料', 22, 1, 5, NULL, 0, 1, 1),
('功能饮料', 22, 1, 6, NULL, 0, 1, 1);

-- 二级分类 - 美妆个护
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('面部护肤', 4, 1, 1, 'https://example.com/icon/skincare.png', 0, 1, 1),
('彩妆', 4, 1, 2, 'https://example.com/icon/makeup.png', 0, 1, 1),
('香水', 4, 1, 3, 'https://example.com/icon/perfume.png', 0, 1, 1),
('个人护理', 4, 1, 4, 'https://example.com/icon/personal.png', 0, 1, 1);

-- 三级分类 - 面部护肤
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('洁面', 25, 1, 1, NULL, 0, 1, 1),
('爽肤水', 25, 1, 2, NULL, 0, 1, 1),
('精华', 25, 1, 3, NULL, 0, 1, 1),
('面霜', 25, 1, 4, NULL, 0, 1, 1),
('面膜', 25, 1, 5, NULL, 0, 1, 1),
('防晒', 25, 1, 6, NULL, 0, 1, 1);

-- 三级分类 - 彩妆
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('粉底', 26, 1, 1, NULL, 0, 1, 1),
('口红', 26, 1, 2, NULL, 0, 1, 1),
('眼影', 26, 1, 3, NULL, 0, 1, 1),
('睫毛膏', 26, 1, 4, NULL, 0, 1, 1),
('腮红', 26, 1, 5, NULL, 0, 1, 1);

-- 二级分类 - 家居用品
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('家纺', 5, 1, 1, 'https://example.com/icon/textile.png', 0, 1, 1),
('家具', 5, 1, 2, 'https://example.com/icon/furniture.png', 0, 1, 1),
('厨具', 5, 1, 3, 'https://example.com/icon/kitchen.png', 0, 1, 1),
('收纳', 5, 1, 4, 'https://example.com/icon/storage.png', 0, 1, 1),
('装饰', 5, 1, 5, 'https://example.com/icon/decor.png', 0, 1, 1);

-- 三级分类 - 家纺
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('四件套', 29, 1, 1, NULL, 0, 1, 1),
('被子', 29, 1, 2, NULL, 0, 1, 1),
('枕头', 29, 1, 3, NULL, 0, 1, 1),
('床垫', 29, 1, 4, NULL, 0, 1, 1),
('窗帘', 29, 1, 5, NULL, 0, 1, 1),
('地毯', 29, 1, 6, NULL, 0, 1, 1);

-- 二级分类 - 运动户外
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('运动服饰', 6, 1, 1, 'https://example.com/icon/sportswear.png', 0, 1, 1),
('运动器材', 6, 1, 2, 'https://example.com/icon/equipment.png', 0, 1, 1),
('户外装备', 6, 1, 3, 'https://example.com/icon/outdoor.png', 0, 1, 1),
('健身器材', 6, 1, 4, 'https://example.com/icon/fitness.png', 0, 1, 1);

-- 三级分类 - 运动服饰
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('运动T恤', 34, 1, 1, NULL, 0, 1, 1),
('运动裤', 34, 1, 2, NULL, 0, 1, 1),
('运动外套', 34, 1, 3, NULL, 0, 1, 1),
('运动内衣', 34, 1, 4, NULL, 0, 1, 1);

-- 三级分类 - 运动器材
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('球类', 35, 1, 1, NULL, 0, 1, 1),
('球拍', 35, 1, 2, NULL, 0, 1, 1),
('护具', 35, 1, 3, NULL, 0, 1, 1);

-- 二级分类 - 图书文教
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('图书', 7, 1, 1, 'https://example.com/icon/books.png', 0, 1, 1),
('文具', 7, 1, 2, 'https://example.com/icon/stationery.png', 0, 1, 1),
('办公用品', 7, 1, 3, 'https://example.com/icon/office.png', 0, 1, 1);

-- 三级分类 - 图书
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('小说', 38, 1, 1, NULL, 0, 1, 1),
('文学', 38, 1, 2, NULL, 0, 1, 1),
('历史', 38, 1, 3, NULL, 0, 1, 1),
('科技', 38, 1, 4, NULL, 0, 1, 1),
('教育', 38, 1, 5, NULL, 0, 1, 1),
('童书', 38, 1, 6, NULL, 0, 1, 1);

-- 二级分类 - 汽车用品
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('汽车装饰', 8, 1, 1, 'https://example.com/icon/car-decor.png', 0, 1, 1),
('汽车配件', 8, 1, 2, 'https://example.com/icon/car-parts.png', 0, 1, 1),
('汽车保养', 8, 1, 3, 'https://example.com/icon/car-care.png', 0, 1, 1);

-- 三级分类 - 汽车装饰
INSERT INTO `product_category` (`name`, `parent_id`, `status`, `sort`, `icon`, `version`, `creator_id`, `updater_id`) VALUES
('座垫', 41, 1, 1, NULL, 0, 1, 1),
('脚垫', 41, 1, 2, NULL, 0, 1, 1),
('方向盘套', 41, 1, 3, NULL, 0, 1, 1),
('挂件', 41, 1, 4, NULL, 0, 1, 1);

-- ==========================================
-- 3. 商品属性表初始化数据（SKU属性名）
-- ==========================================
INSERT INTO `product_property` (`name`, `version`, `creator_id`, `updater_id`) VALUES
('颜色', 0, 1, 1),
('尺寸', 0, 1, 1),
('内存', 0, 1, 1),
('存储', 0, 1, 1),
('屏幕尺寸', 0, 1, 1),
('处理器', 0, 1, 1),
('材质', 0, 1, 1),
('尺码', 0, 1, 1),
('容量', 0, 1, 1),
('重量', 0, 1, 1),
('版本', 0, 1, 1),
('规格', 0, 1, 1);

-- ==========================================
-- 4. 商品属性值表初始化数据（SKU属性值）
-- ==========================================
-- 颜色属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(1, '红色', 0, 1, 1),
(1, '橙色', 0, 1, 1),
(1, '黄色', 0, 1, 1),
(1, '绿色', 0, 1, 1),
(1, '蓝色', 0, 1, 1),
(1, '紫色', 0, 1, 1),
(1, '粉色', 0, 1, 1),
(1, '黑色', 0, 1, 1),
(1, '白色', 0, 1, 1),
(1, '灰色', 0, 1, 1),
(1, '银色', 0, 1, 1),
(1, '金色', 0, 1, 1),
(1, '玫瑰金', 0, 1, 1),
(1, '深空灰', 0, 1, 1),
(1, '星光色', 0, 1, 1);

-- 尺寸属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(2, 'XS', 0, 1, 1),
(2, 'S', 0, 1, 1),
(2, 'M', 0, 1, 1),
(2, 'L', 0, 1, 1),
(2, 'XL', 0, 1, 1),
(2, 'XXL', 0, 1, 1),
(2, 'XXXL', 0, 1, 1);

-- 内存属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(3, '4GB', 0, 1, 1),
(3, '6GB', 0, 1, 1),
(3, '8GB', 0, 1, 1),
(3, '12GB', 0, 1, 1),
(3, '16GB', 0, 1, 1),
(3, '32GB', 0, 1, 1);

-- 存储属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(4, '64GB', 0, 1, 1),
(4, '128GB', 0, 1, 1),
(4, '256GB', 0, 1, 1),
(4, '512GB', 0, 1, 1),
(4, '1TB', 0, 1, 1),
(4, '2TB', 0, 1, 1);

-- 屏幕尺寸属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(5, '5.5英寸', 0, 1, 1),
(5, '6.1英寸', 0, 1, 1),
(5, '6.7英寸', 0, 1, 1),
(5, '13.3英寸', 0, 1, 1),
(5, '14英寸', 0, 1, 1),
(5, '15.6英寸', 0, 1, 1),
(5, '16英寸', 0, 1, 1),
(5, '27英寸', 0, 1, 1),
(5, '32英寸', 0, 1, 1);

-- 处理器属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(6, 'A17 Pro', 0, 1, 1),
(6, 'A16 Bionic', 0, 1, 1),
(6, 'A15 Bionic', 0, 1, 1),
(6, '骁龙8 Gen 3', 0, 1, 1),
(6, '骁龙8 Gen 2', 0, 1, 1),
(6, '天玑9300', 0, 1, 1),
(6, 'Intel i5', 0, 1, 1),
(6, 'Intel i7', 0, 1, 1),
(6, 'Intel i9', 0, 1, 1),
(6, 'AMD Ryzen 5', 0, 1, 1),
(6, 'AMD Ryzen 7', 0, 1, 1),
(6, 'AMD Ryzen 9', 0, 1, 1);

-- 材质属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(7, '纯棉', 0, 1, 1),
(7, '涤纶', 0, 1, 1),
(7, '丝绸', 0, 1, 1),
(7, '羊毛', 0, 1, 1),
(7, '真皮', 0, 1, 1),
(7, 'PU皮', 0, 1, 1),
(7, '帆布', 0, 1, 1),
(7, '尼龙', 0, 1, 1),
(7, '金属', 0, 1, 1),
(7, '塑料', 0, 1, 1),
(7, '玻璃', 0, 1, 1),
(7, '陶瓷', 0, 1, 1);

-- 尺码属性值（服装）
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(8, '160/84A', 0, 1, 1),
(8, '165/88A', 0, 1, 1),
(8, '170/92A', 0, 1, 1),
(8, '175/96A', 0, 1, 1),
(8, '180/100A', 0, 1, 1),
(8, '185/104A', 0, 1, 1),
(8, '36码', 0, 1, 1),
(8, '37码', 0, 1, 1),
(8, '38码', 0, 1, 1),
(8, '39码', 0, 1, 1),
(8, '40码', 0, 1, 1),
(8, '41码', 0, 1, 1),
(8, '42码', 0, 1, 1),
(8, '43码', 0, 1, 1),
(8, '44码', 0, 1, 1);

-- 容量属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(9, '100ml', 0, 1, 1),
(9, '200ml', 0, 1, 1),
(9, '250ml', 0, 1, 1),
(9, '500ml', 0, 1, 1),
(9, '750ml', 0, 1, 1),
(9, '1000ml', 0, 1, 1),
(9, '1.5L', 0, 1, 1),
(9, '2L', 0, 1, 1),
(9, '5L', 0, 1, 1);

-- 重量属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(10, '100g', 0, 1, 1),
(10, '200g', 0, 1, 1),
(10, '250g', 0, 1, 1),
(10, '500g', 0, 1, 1),
(10, '1kg', 0, 1, 1),
(10, '2kg', 0, 1, 1),
(10, '5kg', 0, 1, 1),
(10, '10kg', 0, 1, 1);

-- 版本属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(11, '标准版', 0, 1, 1),
(11, '专业版', 0, 1, 1),
(11, '旗舰版', 0, 1, 1),
(11, '青春版', 0, 1, 1),
(11, 'Plus版', 0, 1, 1),
(11, 'Pro版', 0, 1, 1),
(11, 'Max版', 0, 1, 1);

-- 规格属性值
INSERT INTO `product_property_value` (`property_id`, `name`, `version`, `creator_id`, `updater_id`) VALUES
(12, '单件装', 0, 1, 1),
(12, '双件装', 0, 1, 1),
(12, '三件装', 0, 1, 1),
(12, '套装', 0, 1, 1),
(12, '礼盒装', 0, 1, 1),
(12, '家庭装', 0, 1, 1),
(12, '经济装', 0, 1, 1),
(12, '豪华装', 0, 1, 1);

