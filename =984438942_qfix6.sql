SET QUOTED_IDENTIFIER ON;
UPDATE exercises SET correct_answer=N'beef', updated_at=GETDATE() WHERE exercise_id=650838;
UPDATE exercises SET correct_answer=N'Mathematics', updated_at=GETDATE() WHERE488 exercise_id=651164;
UPDATE exercises SET correct_answer=N'Clarinet', updated_at=GETDATE() WHERE exercise_id=651604;
UPDATE exercises SET correct_answer=N'A] I am writing', updated_at=GETDATE() WHERE exercise_id=661972;
UPDATE exercises SET correct_answer=N'A. My new home is an apartment with three bedrooms, a living room, and a kitchen.', updated_at=GETDATE() WHERE exercise_id=745643;
UPDATE exercises SET correct_answer=N'A. I am a quiet and introverted person who enjoys reading books and watching movies in my free time.', updated_at=GETDATE() WHERE exercise_id=745708;
SELECT (SELECT COUNT(*) FROM exercises WHERE exercise_id IN (650838,651164,651604,661972,745643,745708) AND correct_answer NOT IN ('A','B','C','D','a','b','c','d')) AS all_fixed;