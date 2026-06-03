-- =============================================
-- data.sql - Dữ liệu mẫu cho EngFlow
-- Chỉ INSERT nếu bảng rỗng (tránh duplicate khi restart)
-- =============================================

-- Seed Users (Password: 123456)
IF NOT EXISTS (SELECT 1 FROM users)
BEGIN
    INSERT INTO users (username, email, password_hash, full_name, avatar_url, role, current_level, total_points, is_active)
    VALUES 
    ('student', 'user@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Học Viên Mẫu', 'https://api.dicebear.com/7.x/adventurer/svg?seed=student', 'USER', 'BEGINNER', 0, 1),
    ('administrator', 'admin@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Quản Trị Viên', 'https://api.dicebear.com/7.x/adventurer/svg?seed=admin', 'ADMIN', 'ADVANCED', 0, 1);
END;

-- Seed Lessons
IF NOT EXISTS (SELECT 1 FROM lessons)
BEGIN
    SET IDENTITY_INSERT lessons ON;
    INSERT INTO lessons (lesson_id, title, description, content, level, category, duration_minutes, thumbnail_url, audio_url, order_index, is_published)
    VALUES 
    (1, N'Chào hỏi và Giới thiệu bản thân', N'Học cách chào hỏi thông dụng và giới thiệu bản thân bằng tiếng Anh cơ bản.', N'Chào hỏi là bước đầu tiên và quan trọng nhất khi giao tiếp bằng tiếng Anh. Trong bài học này, chúng ta sẽ học các mẫu câu chào hỏi trang trọng và thân mật, cách tự giới thiệu tên, tuổi, quê quán của mình.', 'BEGINNER', 'Communication', 15, 'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=500', '', 1, 1),
    (2, N'Thói quen Hằng ngày', N'Mở rộng vốn từ vựng về các hoạt động thường ngày và thì Hiện tại đơn.', N'Thói quen hằng ngày mô tả những việc chúng ta làm lặp đi lặp lại mỗi ngày. Chúng ta sẽ làm quen với các động từ chỉ hoạt động và cấu trúc câu mô tả thói quen.', 'BEGINNER', 'Vocabulary', 20, 'https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500', '', 2, 1),
    (3, N'Du lịch và Hỏi đường', N'Học từ vựng liên quan đến du lịch, sân bay và các mẫu câu hỏi đường thông dụng.', N'Khi đi du lịch nước ngoài, khả năng giao tiếp tại sân bay và hỏi đường là cực kỳ cần thiết. Bài học này sẽ giúp bạn tự tin di chuyển ở các địa điểm công cộng.', 'INTERMEDIATE', 'Travel', 25, 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=500', '', 3, 1),
    (4, N'Mua sắm và Trả giá', N'Từ vựng về quần áo, giá cả và các mẫu câu giao tiếp khi mua sắm.', N'Mua sắm là một hoạt động phổ biến khi du lịch hoặc sinh sống ở nước ngoài. Bài học này trang bị cho bạn các từ vựng, mẫu câu hỏi giá, trả giá, và thanh toán.', 'BEGINNER', 'Shopping', 20, 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=500', '', 4, 1),
    (5, N'Đồ ăn và Nhà hàng', N'Học cách gọi món, đọc thực đơn và giao tiếp tại nhà hàng.', N'Thưởng thức ẩm thực là một phần quan trọng khi trải nghiệm văn hóa mới. Bài học này giúp bạn tự tin gọi món, hỏi thông tin về thành phần, và thanh toán hóa đơn tại nhà hàng.', 'INTERMEDIATE', 'Food', 25, 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500', '', 5, 1),
    (6, N'Phỏng vấn xin việc', N'Luyện kỹ năng trả lời phỏng vấn bằng tiếng Anh chuyên nghiệp.', N'Phỏng vấn xin việc bằng tiếng Anh đòi hỏi sự chuẩn bị kỹ lưỡng. Bài học này hướng dẫn cách trả lời các câu hỏi phổ biến, mô tả kinh nghiệm làm việc, và thể hiện bản thân một cách chuyên nghiệp.', 'ADVANCED', 'Business', 30, 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=500', '', 6, 1);
    SET IDENTITY_INSERT lessons OFF;
END;

-- Seed Vocabulary
IF NOT EXISTS (SELECT 1 FROM vocabulary)
BEGIN
    INSERT INTO vocabulary (lesson_id, word, pronunciation, meaning, example_sentence, audio_url, image_url, word_type)
    VALUES 
    (1, 'Introduce', '/intr@djuus/', N'Giới thiệu', 'Let me introduce myself. I am Nam.', '', '', 'Verb'),
    (1, 'Pleasure', '/plezh@r/', N'Niềm vinh hạnh', 'It is a pleasure to meet you.', '', '', 'Noun'),
    (1, 'Greeting', '/griiting/', N'Lời chào hỏi', 'In English, Hello is a common greeting.', '', '', 'Noun'),
    (1, 'Acquaintance', '/@kweint@ns/', N'Người quen', 'She is an acquaintance of mine from work.', '', '', 'Noun'),
    (1, 'Farewell', '/fe@wel/', N'Lời tạm biệt', 'We said our farewells at the airport.', '', '', 'Noun'),
    (2, 'Wake up', '/weik up/', N'Thức giấc', 'I usually wake up at 6 AM.', '', '', 'Verb'),
    (2, 'Routine', '/ruutiin/', N'Thói quen hằng ngày', 'Getting exercise is part of my daily routine.', '', '', 'Noun'),
    (2, 'Breakfast', '/brekf@st/', N'Bữa ăn sáng', 'I have bread and milk for breakfast.', '', '', 'Noun'),
    (2, 'Commute', '/k@mjuut/', N'Đi lại hằng ngày', 'My daily commute takes about 30 minutes.', '', '', 'Verb'),
    (2, 'Schedule', '/shedjuul/', N'Lịch trình', 'I have a very tight schedule today.', '', '', 'Noun'),
    (3, 'Departure', '/dipaatech@r/', N'Sự khởi hành', 'Please check the departure board for your flight.', '', '', 'Noun'),
    (3, 'Destination', '/destineiishn/', N'Điểm đến', 'Paris is a popular tourist destination.', '', '', 'Noun'),
    (3, 'Luggage', '/lugij/', N'Hành lý', 'We need to check in our luggage.', '', '', 'Noun'),
    (3, 'Boarding pass', '/boarding paas/', N'Thẻ lên máy bay', 'Please show your boarding pass at the gate.', '', '', 'Noun'),
    (3, 'Itinerary', '/aitiner@ri/', N'Lịch trình chuyến đi', 'Our itinerary includes visits to three cities.', '', '', 'Noun'),
    (4, 'Discount', '/diskaunt/', N'Giảm giá', 'There is a 20 percent discount on all items today.', '', '', 'Noun'),
    (4, 'Receipt', '/risiit/', N'Hóa đơn', 'Could I have the receipt, please?', '', '', 'Noun'),
    (4, 'Fitting room', '/fiting ruum/', N'Phòng thử đồ', 'The fitting room is over there.', '', '', 'Noun'),
    (4, 'Bargain', '/baagin/', N'Mặc cả', 'She likes to bargain at the market.', '', '', 'Verb'),
    (4, 'Refund', '/riifund/', N'Hoàn tiền', 'I would like a refund for this defective item.', '', '', 'Noun'),
    (5, 'Appetizer', '/apitaiz@r/', N'Món khai vị', 'I will have a salad as an appetizer.', '', '', 'Noun'),
    (5, 'Beverage', '/bev@rij/', N'Đồ uống', 'What beverage would you like to order?', '', '', 'Noun'),
    (5, 'Ingredient', '/ingriidie@nt/', N'Nguyên liệu', 'This dish uses fresh local ingredients.', '', '', 'Noun'),
    (5, 'Reservation', '/rez@veishn/', N'Đặt chỗ', 'I would like to make a reservation for two.', '', '', 'Noun'),
    (5, 'Tip', '/tip/', N'Tiền boa', 'It is customary to leave a 15 percent tip in America.', '', '', 'Noun'),
    (6, 'Qualification', '/kwolifikeishn/', N'Bằng cấp', 'What qualifications do you have for this role?', '', '', 'Noun'),
    (6, 'Responsibility', '/risponsi biliti/', N'Trách nhiệm', 'My main responsibility was managing the team.', '', '', 'Noun'),
    (6, 'Achievement', '/@chiivment/', N'Thành tựu', 'My greatest achievement was launching the product.', '', '', 'Noun'),
    (6, 'Salary', '/sal@ri/', N'Lương', 'The salary for this position is competitive.', '', '', 'Noun'),
    (6, 'Deadline', '/dedlain/', N'Hạn chót', 'I always meet my deadlines on time.', '', '', 'Noun');
END;

-- Seed Grammar
IF NOT EXISTS (SELECT 1 FROM grammar)
BEGIN
    INSERT INTO grammar (lesson_id, title, explanation, formula, examples, level)
    VALUES 
    (1, N'Động từ To Be (am, is, are)', N'Sử dụng để giới thiệu tên, tuổi, nghề nghiệp, quốc tịch.', 'S + am/is/are + Noun/Adj', N'I am a student. He is friendly. They are from Vietnam.', 'BEGINNER'),
    (1, N'Đại từ nhân xưng', N'Các đại từ dùng để thay thế cho danh từ chỉ người, vật trong câu.', 'I / You / He / She / It / We / They', N'I am a teacher. He is my friend. We are classmates.', 'BEGINNER'),
    (2, N'Thì Hiện tại đơn', N'Dùng để diễn tả thói quen hằng ngày, hành động lặp lại hoặc sự thật hiển nhiên.', 'S + V(s/es)', N'She wakes up early. They do not work on Sundays.', 'BEGINNER'),
    (2, N'Trạng từ chỉ tần suất', N'Mô tả mức độ thường xuyên: always, usually, often, sometimes, rarely, never.', 'S + adverb + V', N'I always brush my teeth. She is usually on time.', 'BEGINNER'),
    (3, N'Động từ khuyết thiếu (Can, Should, Must)', N'Dùng để chỉ khả năng, lời khuyên hoặc sự bắt buộc.', 'S + Modal Verb + V_inf', N'You can take the bus. You should ask for directions.', 'INTERMEDIATE'),
    (3, N'Câu hỏi với Where/How', N'Dùng để hỏi về địa điểm hoặc cách thức di chuyển.', 'Where + be + S? / How + do/does + S + get to + place?', N'Where is the nearest hospital? How do I get to the station?', 'INTERMEDIATE'),
    (4, N'Câu so sánh hơn', N'Dùng để so sánh hai sự vật. Thêm -er hoặc dùng more.', 'S1 + be + Adj-er + than + S2', N'This shirt is cheaper than that one.', 'BEGINNER'),
    (5, N'Câu hỏi lịch sự', N'Dùng cấu trúc lịch sự để yêu cầu hoặc hỏi thông tin.', 'Could I / Could you + V_inf?', N'Could I have the menu, please? I would like to order a steak.', 'INTERMEDIATE'),
    (5, N'Danh từ đếm được và không đếm được', N'Phân biệt danh từ có thể đếm và không đếm.', 'Countable: a/an/many + N / Uncountable: some/much + N', N'I would like two cups of coffee. How much sugar do you want?', 'INTERMEDIATE'),
    (6, N'Thì Hiện tại hoàn thành', N'Dùng để nói về kinh nghiệm, thành tựu.', 'S + have/has + V_past_participle', N'I have worked in IT for 5 years. She has managed a team.', 'ADVANCED'),
    (6, N'Câu điều kiện loại 2', N'Dùng để nói về tình huống giả định ở hiện tại.', 'If + S + V_past, S + would + V_inf', N'If I were the manager, I would improve the process.', 'ADVANCED');
END;

-- Seed Exercises
IF NOT EXISTS (SELECT 1 FROM exercises)
BEGIN
    INSERT INTO exercises (lesson_id, title, question, exercise_type, options, correct_answer, explanation, points, difficulty, audio_url)
    VALUES 
    (1, N'Chọn từ chào hỏi phù hợp', N'Khi gặp một người bạn mới vào buổi sáng, bạn sẽ chào thế nào?', 'MULTIPLE_CHOICE', '["Good night", "Good morning", "Goodbye", "See you later"]', '1', N'Good morning là lời chào buổi sáng.', 10, 'BEGINNER', ''),
    (1, N'Điền động từ To Be', N'I _____ from Vietnam. (am/is/are)', 'FILL_IN_BLANK', '["am"]', 'am', N'Chủ ngữ I đi kèm với am.', 10, 'BEGINNER', ''),
    (1, N'Chọn cách giới thiệu đúng', N'Bạn muốn giới thiệu tên mình, câu nào đúng?', 'MULTIPLE_CHOICE', '["My name am John.", "I name is John.", "My name is John.", "Name my is John."]', '2', N'Cấu trúc đúng: My name is + tên.', 10, 'BEGINNER', ''),
    (2, N'Chia động từ thích hợp', N'My father always _____ (drink) tea in the morning.', 'FILL_IN_BLANK', '["drinks"]', 'drinks', N'Ngôi thứ 3 số ít thêm s.', 10, 'BEGINNER', ''),
    (2, N'Chọn câu đúng', N'Chọn câu phủ định đúng:', 'MULTIPLE_CHOICE', '["He do not play football.", "He does not plays football.", "He does not play football.", "He is not play football."]', '2', N'does not + động từ nguyên thể.', 10, 'BEGINNER', ''),
    (2, N'Điền trạng từ tần suất', N'She _____ goes to the gym. She goes five days a week.', 'MULTIPLE_CHOICE', '["never", "rarely", "usually", "sometimes"]', '2', N'usually phù hợp vì đi 5 ngày/tuần.', 10, 'BEGINNER', ''),
    (3, N'Chọn câu hỏi đường phù hợp', N'Bạn muốn hỏi đường tới ga tàu, câu nào lịch sự và đúng?', 'MULTIPLE_CHOICE', '["Where station?", "How can I get to the nearest station?", "Station is where?", "Show me station."]', '1', N'How can I get to... là cách hỏi đường lịch sự.', 15, 'INTERMEDIATE', ''),
    (3, N'Điền từ phù hợp', N'You _____ show your passport at the immigration desk.', 'FILL_IN_BLANK', '["must"]', 'must', N'must diễn tả sự bắt buộc.', 15, 'INTERMEDIATE', ''),
    (3, N'Chọn từ đúng', N'All passengers must go through _____ before boarding.', 'MULTIPLE_CHOICE', '["security check", "luggage room", "waiting hall", "parking lot"]', '0', N'Security check là bước bắt buộc trước khi lên máy bay.', 15, 'INTERMEDIATE', ''),
    (4, N'Hỏi giá sản phẩm', N'Bạn muốn hỏi giá một chiếc áo, câu nào đúng?', 'MULTIPLE_CHOICE', '["What money this?", "How much is this shirt?", "This shirt money?", "Price of shirt tell me."]', '1', N'How much is this shirt? là cách hỏi giá đúng.', 10, 'BEGINNER', ''),
    (4, N'Điền từ phù hợp', N'Could I try this dress on in the _____?', 'FILL_IN_BLANK', '["fitting room"]', 'fitting room', N'fitting room là phòng thử đồ.', 10, 'BEGINNER', ''),
    (4, N'Thanh toán bằng thẻ', N'Người bán nói: That will be $25. Bạn muốn trả bằng thẻ?', 'MULTIPLE_CHOICE', '["I pay card.", "Can I pay by card?", "Card money please.", "Give card you."]', '1', N'Can I pay by card? là cách hỏi lịch sự.', 10, 'BEGINNER', ''),
    (5, N'Gọi món tại nhà hàng', N'Cách gọi món lịch sự nhất?', 'MULTIPLE_CHOICE', '["Give me steak.", "I would like a steak, please.", "Steak! Now!", "I want eat steak."]', '1', N'I would like... please là cách gọi lịch sự nhất.', 15, 'INTERMEDIATE', ''),
    (5, N'Điền từ phù hợp', N'Could I have the _____, please? I want to see what you serve.', 'FILL_IN_BLANK', '["menu"]', 'menu', N'menu là thực đơn.', 15, 'INTERMEDIATE', ''),
    (5, N'Danh từ không đếm được', N'Từ nào là danh từ KHÔNG đếm được?', 'MULTIPLE_CHOICE', '["apple", "water", "egg", "sandwich"]', '1', N'water là danh từ không đếm được.', 15, 'INTERMEDIATE', ''),
    (6, N'Trả lời phỏng vấn', N'Tell me about yourself - cách trả lời chuyên nghiệp nhất?', 'MULTIPLE_CHOICE', '["I like playing games.", "I have 3 years of experience in marketing.", "I am fine, thank you.", "My name is Minh. I like cats."]', '1', N'Câu trả lời nên tập trung vào kinh nghiệm.', 20, 'ADVANCED', ''),
    (6, N'Điền thì đúng', N'I _____ (work) in the IT industry for 5 years.', 'FILL_IN_BLANK', '["have worked"]', 'have worked', N'Dùng Present Perfect cho kinh nghiệm.', 20, 'ADVANCED', ''),
    (6, N'Câu điều kiện loại 2', N'If I _____ the team leader, I would improve communication.', 'FILL_IN_BLANK', '["were"]', 'were', N'Câu điều kiện loại 2 dùng were cho tất cả các ngôi.', 20, 'ADVANCED', '');
END;

-- Seed Achievements
IF NOT EXISTS (SELECT 1 FROM achievements)
BEGIN
    SET IDENTITY_INSERT achievements ON;
    INSERT INTO achievements (achievement_id, name, description, icon_url, points_required, badge_type)
    VALUES 
    (1, N'Khởi đầu Thuận lợi', N'Hoàn thành bài học đầu tiên.', 'https://api.dicebear.com/7.x/identicon/svg?seed=first-step', 10, 'BRONZE'),
    (2, N'Chăm Chỉ', N'Tích lũy 30 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=diligence', 30, 'SILVER'),
    (3, N'Chuyên Gia Ngôn Ngữ', N'Tích lũy 50 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=expert', 50, 'GOLD'),
    (4, N'Người Kiên Trì', N'Tích lũy 100 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=persistence', 100, 'GOLD'),
    (5, N'Bậc Thầy Tiếng Anh', N'Tích lũy 200 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=master', 200, 'DIAMOND');
    SET IDENTITY_INSERT achievements OFF;
END;
