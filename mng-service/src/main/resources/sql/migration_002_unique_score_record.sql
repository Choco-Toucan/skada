-- 分数记录表增加唯一约束，防止并发重复上报
-- 一个用户在同一个排行榜实例中对同一个指标只能有一条记录
ALTER TABLE score_record
ADD UNIQUE KEY uk_user_instance_metric (leaderboard_id, instance_id, user_id, metric_id);
