-- Seed categories
INSERT INTO categories (name) VALUES
    ('Đồ uống'),
    ('Đồ ăn nhanh'),
    ('Snack'),
    ('Khác');

-- Seed products (20 items, users are seeded programmatically by DataInitializerConfig)
INSERT INTO products (name, description, price, stock, image_url, category_id) VALUES
    -- Đồ uống (id=1)
    ('Trà sữa trân châu',      'Trà sữa thơm ngon với trân châu dai mềm',              35000,  100, 'https://picsum.photos/300/300?random=1',  1),
    ('Cà phê đen đá',          'Cà phê đen Việt Nam truyền thống pha phin',             25000,  150, 'https://picsum.photos/300/300?random=2',  1),
    ('Nước cam ép tươi',        'Nước cam tươi ép lạnh, không đường',                  30000,   80, 'https://picsum.photos/300/300?random=3',  1),
    ('Sinh tố bơ',              'Sinh tố bơ béo ngậy thơm lừng, thêm sữa đặc',        45000,   60, 'https://picsum.photos/300/300?random=4',  1),
    ('Nước suối Lavie 500ml',   'Nước suối tinh khiết, thanh mát',                      10000,  300, 'https://picsum.photos/300/300?random=5',  1),
    ('Hồng trà chanh',          'Hồng trà pha chanh tươi, thêm đá',                    28000,  120, 'https://picsum.photos/300/300?random=6',  1),

    -- Đồ ăn nhanh (id=2)
    ('Burger bò phô mai',       'Burger bò kẹp phô mai Cheddar và rau sống tươi',      55000,   50, 'https://picsum.photos/300/300?random=7',  2),
    ('Hotdog xúc xích Đức',     'Hotdog với xúc xích Đức kèm sốt mù tạt và tương cà', 40000,   70, 'https://picsum.photos/300/300?random=8',  2),
    ('Bánh mì thịt nguội pate', 'Bánh mì giòn kẹp thịt nguội, pate và dưa leo',       30000,  100, 'https://picsum.photos/300/300?random=9',  2),
    ('Mì ly Hảo Hảo tôm chua', 'Mì ăn liền hương vị tôm chua cay đặc trưng',           8000,  300, 'https://picsum.photos/300/300?random=10', 2),
    ('Cơm hộp gà nướng sa tế',  'Cơm gà nướng sa tế thơm lừng kèm rau cải',           65000,   30, 'https://picsum.photos/300/300?random=11', 2),

    -- Snack (id=3)
    ('Pringles phô mai',        'Khoai tây chiên giòn vị phô mai, lon 165g',            35000,  120, 'https://picsum.photos/300/300?random=12', 3),
    ('Bánh Oreo vani',          'Bánh quy sandwich nhân kem vani, gói 133g',            28000,  150, 'https://picsum.photos/300/300?random=13', 3),
    ('Snack tôm Oishi',         'Snack tôm giòn tan vị đặc biệt, gói 60g',             15000,  200, 'https://picsum.photos/300/300?random=14', 3),
    ('Kẹo dẻo Haribo gấu',     'Kẹo dẻo hình gấu đủ màu sắc, túi 200g',              45000,  100, 'https://picsum.photos/300/300?random=15', 3),
    ('Bắp rang bơ',             'Bắp rang bơ thơm ngon giòn rụm, túi lớn',             20000,   80, 'https://picsum.photos/300/300?random=16', 3),

    -- Khác (id=4)
    ('Khăn giấy Kleenex 3 lớp', 'Khăn giấy mềm mại 3 lớp, hộp 200 tờ',              25000,  200, 'https://picsum.photos/300/300?random=17', 4),
    ('Pin AA Duracell',          'Pin kiềm AA dùng lâu, vỉ 4 viên',                    35000,  100, 'https://picsum.photos/300/300?random=18', 4),
    ('Kem đánh răng Colgate',    'Bảo vệ men răng toàn diện 200g',                     45000,   80, 'https://picsum.photos/300/300?random=19', 4),
    ('Băng dán cá nhân',         'Băng dán vết thương kháng khuẩn, hộp 10 cái',       20000,  150, 'https://picsum.photos/300/300?random=20', 4);
