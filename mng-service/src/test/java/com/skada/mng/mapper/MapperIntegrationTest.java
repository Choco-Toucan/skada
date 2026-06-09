package com.skada.mng.mapper;

import com.skada.mng.model.*;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Mapper 集成测试")
class MapperIntegrationTest {

    private static SqlSessionFactory sqlSessionFactory;

    private TenantMapper tenantMapper;
    private MetricMapper metricMapper;
    private LeaderboardMapper leaderboardMapper;
    private LeaderboardInstanceMapper instanceMapper;
    private LeaderboardMetricMapper leaderboardMetricMapper;
    private ScoreRecordMapper scoreRecordMapper;
    private AdminUserMapper adminUserMapper;

    @BeforeAll
    static void initDatabase() throws Exception {
        DataSource dataSource = new PooledDataSource(
                "org.h2.Driver",
                "jdbc:h2:mem:skada_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa", "");

        try (Connection conn = dataSource.getConnection()) {
            RunScript.execute(conn,
                    new InputStreamReader(MapperIntegrationTest.class
                            .getResourceAsStream("/schema-h2.sql")));
        }

        Environment environment = new Environment("test",
                new JdbcTransactionFactory(), dataSource);
        Configuration config = new Configuration(environment);
        config.setMapUnderscoreToCamelCase(true);

        // 注册所有 mapper（自动发现同名 XML）
        config.addMappers("com.skada.mng.mapper");

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
    }

    @BeforeEach
    void setUp() {
        SqlSession session = sqlSessionFactory.openSession();
        tenantMapper = session.getMapper(TenantMapper.class);
        metricMapper = session.getMapper(MetricMapper.class);
        leaderboardMapper = session.getMapper(LeaderboardMapper.class);
        instanceMapper = session.getMapper(LeaderboardInstanceMapper.class);
        leaderboardMetricMapper = session.getMapper(LeaderboardMetricMapper.class);
        scoreRecordMapper = session.getMapper(ScoreRecordMapper.class);
        adminUserMapper = session.getMapper(AdminUserMapper.class);
    }

    @Nested
    @DisplayName("TenantMapper")
    class TenantMapperTests {

        @Test
        @DisplayName("insert 后再 findById 能找到")
        void insertAndFindById() {
            Tenant t = createTenant("tn_test123", "测试租户", "sk_test");
            tenantMapper.insert(t);
            assertThat(t.getId()).isNotNull();

            Tenant found = tenantMapper.findById(t.getId());
            assertThat(found.getTenantId()).isEqualTo("tn_test123");
            assertThat(found.getName()).isEqualTo("测试租户");
        }

        @Test
        @DisplayName("findByTenantId 不存在返回 null")
        void findByTenantIdNotFound() {
            assertThat(tenantMapper.findByTenantId("tn_nonexistent")).isNull();
        }

        @Test
        @DisplayName("update 更新租户信息")
        void update() {
            Tenant t = createTenant("tn_upd", "旧名称", "sk_upd");
            tenantMapper.insert(t);
            t.setName("新名称");
            tenantMapper.update(t);

            assertThat(tenantMapper.findById(t.getId()).getName()).isEqualTo("新名称");
        }

        @Test
        @DisplayName("分页查询和 count")
        void paginationAndCount() {
            long count = tenantMapper.count();
            tenantMapper.insert(createTenant("tn_pg", "PG", "sk_pg"));
            List<Tenant> page = tenantMapper.findAllWithPage(0, 10);
            assertThat(page).isNotEmpty();
            assertThat(tenantMapper.count()).isEqualTo(count + 1);
        }
    }

    @Nested
    @DisplayName("MetricMapper")
    class MetricMapperTests {

        @Test
        @DisplayName("insert 后 findById 能找到")
        void insertAndFindById() {
            Metric m = createMetric("mt_test001", "tn_test", "击杀数");
            metricMapper.insert(m);
            assertThat(m.getId()).isNotNull();

            Metric found = metricMapper.findById(m.getId());
            assertThat(found.getMetricId()).isEqualTo("mt_test001");
        }

        @Test
        @DisplayName("findByMetricId 返回正确记录")
        void findByMetricId() {
            metricMapper.insert(createMetric("mt_mi001", "tn_test", "得分"));
            assertThat(metricMapper.findByMetricId("mt_mi001").getName()).isEqualTo("得分");
        }

        @Test
        @DisplayName("update 更新指标")
        void update() {
            Metric m = createMetric("mt_upd", "tn_test", "旧");
            metricMapper.insert(m);
            m.setName("新名称");
            metricMapper.update(m);
            assertThat(metricMapper.findById(m.getId()).getName()).isEqualTo("新名称");
        }

        @Test
        @DisplayName("deleteById 删除指标")
        void deleteById() {
            Metric m = createMetric("mt_del", "tn_test", "待删除");
            metricMapper.insert(m);
            metricMapper.deleteById(m.getId());
            assertThat(metricMapper.findById(m.getId())).isNull();
        }
    }

    @Nested
    @DisplayName("LeaderboardMapper")
    class LeaderboardMapperTests {

        @Test
        @DisplayName("insert 后 findById 和 findByPlanId 能找到")
        void insertAndFind() {
            Leaderboard lb = buildLeaderboard("lb_001", "tn_test");
            leaderboardMapper.insert(lb);
            assertThat(lb.getId()).isNotNull();

            assertThat(leaderboardMapper.findById(lb.getId()).getPlanId()).isEqualTo("lb_001");
            assertThat(leaderboardMapper.findByPlanId("lb_001")).isNotNull();
        }

        @Test
        @DisplayName("findByTenantId 按租户查询")
        void findByTenantId() {
            leaderboardMapper.insert(buildLeaderboard("lb_a1", "tn_a"));
            leaderboardMapper.insert(buildLeaderboard("lb_a2", "tn_a"));
            assertThat(leaderboardMapper.findByTenantId("tn_a")).hasSize(2);
        }

        @Test
        @DisplayName("分页查询和 countByTenantId")
        void pagination() {
            leaderboardMapper.insert(buildLeaderboard("lb_p1", "tn_page"));
            leaderboardMapper.insert(buildLeaderboard("lb_p2", "tn_page"));

            assertThat(leaderboardMapper.findByTenantIdWithPage("tn_page", 0, 1)).hasSize(1);
            assertThat(leaderboardMapper.countByTenantId("tn_page")).isEqualTo(2);
        }

        @Test
        @DisplayName("update 更新排行榜")
        void update() {
            Leaderboard lb = buildLeaderboard("lb_upd", "tn_test");
            leaderboardMapper.insert(lb);
            lb.setName("已更新");
            lb.setStatus("stopped");
            leaderboardMapper.update(lb);

            Leaderboard updated = leaderboardMapper.findById(lb.getId());
            assertThat(updated.getName()).isEqualTo("已更新");
            assertThat(updated.getStatus()).isEqualTo("stopped");
        }
    }

    @Nested
    @DisplayName("LeaderboardInstanceMapper")
    class LeaderboardInstanceMapperTests {

        @Test
        @DisplayName("findActiveByLeaderboardId 找到活跃实例")
        void findActive() {
            instanceMapper.insert(buildInstance(10L, "li_act", 1, "active"));
            assertThat(instanceMapper.findActiveByLeaderboardId(10L)).isNotNull();
        }

        @Test
        @DisplayName("关闭后再查活跃实例返回 null")
        void closeThenFindActiveReturnsNull() {
            LeaderboardInstance inst = buildInstance(20L, "li_cls", 1, "active");
            instanceMapper.insert(inst);
            instanceMapper.closeInstance(inst.getId(), System.currentTimeMillis());
            assertThat(instanceMapper.findActiveByLeaderboardId(20L)).isNull();
        }

        @Test
        @DisplayName("getMaxInstanceSeq 返回最大序号")
        void maxSeq() {
            instanceMapper.insert(buildInstance(30L, "li_s1", 1, "closed"));
            instanceMapper.insert(buildInstance(30L, "li_s2", 2, "active"));
            assertThat(instanceMapper.getMaxInstanceSeq(30L)).isEqualTo(2);
        }

        @Test
        @DisplayName("findByLeaderboardId 返回所有实例")
        void findAll() {
            instanceMapper.insert(buildInstance(40L, "li_h1", 1, "closed"));
            instanceMapper.insert(buildInstance(40L, "li_h2", 2, "active"));
            assertThat(instanceMapper.findByLeaderboardId(40L)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("LeaderboardMetricMapper")
    class LeaderboardMetricMapperTests {

        @Test
        @DisplayName("insertBatch 后 findByLeaderboardId 能找到")
        void insertBatchAndFind() {
            leaderboardMetricMapper.insertBatch(List.of(
                    buildMetricAssoc(50L, 10L, 1, "desc"),
                    buildMetricAssoc(50L, 20L, 2, "asc")));
            assertThat(leaderboardMetricMapper.findByLeaderboardId(50L)).hasSize(2);
        }

        @Test
        @DisplayName("deleteByLeaderboardId 删除后为空")
        void delete() {
            leaderboardMetricMapper.insertBatch(List.of(buildMetricAssoc(60L, 30L, 1, "desc")));
            leaderboardMetricMapper.deleteByLeaderboardId(60L);
            assertThat(leaderboardMetricMapper.findByLeaderboardId(60L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ScoreRecordMapper")
    class ScoreRecordMapperTests {

        @Test
        @DisplayName("findByInstance 无数据时返回空列表")
        void findByInstanceEmpty() {
            List<ScoreRecord> result = scoreRecordMapper.findByInstance(999L, 999L);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("AdminUserMapper")
    class AdminUserMapperTests {

        @Test
        @DisplayName("findByUsername 不存在的用户返回 null")
        void findByUsernameNotFound() {
            assertThat(adminUserMapper.findByUsername("no_such_user")).isNull();
        }
    }

    // ===== helpers =====

    private Tenant createTenant(String tenantId, String name, String secretKey) {
        Tenant t = new Tenant();
        t.setTenantId(tenantId);
        t.setName(name);
        t.setSecretKey(secretKey);
        t.setAllowAnonymousQuery(false);
        t.setStatus(1);
        t.setCreateBy("system");
        t.setUpdateBy("system");
        return t;
    }

    private Metric createMetric(String metricId, String tenantId, String name) {
        Metric m = new Metric();
        m.setMetricId(metricId);
        m.setTenantId(tenantId);
        m.setName(name);
        m.setCreateBy("system");
        m.setUpdateBy("system");
        return m;
    }

    private Leaderboard buildLeaderboard(String planId, String tenantId) {
        Leaderboard lb = new Leaderboard();
        lb.setPlanId(planId);
        lb.setTenantId(tenantId);
        lb.setName("测试榜");
        lb.setStartTime(1000000L);
        lb.setMaxQueryUsers(1000);
        lb.setAllowDuplicateReport(0);
        lb.setAllowHistoryQuery(1);
        lb.setRollStrategy("none");
        lb.setStatus("active");
        lb.setCreateBy("system");
        lb.setUpdateBy("system");
        return lb;
    }

    private LeaderboardInstance buildInstance(Long leaderboardId, String instanceId, int seq, String status) {
        LeaderboardInstance inst = new LeaderboardInstance();
        inst.setInstanceId(instanceId);
        inst.setLeaderboardId(leaderboardId);
        inst.setInstanceSeq(seq);
        inst.setStartTime(System.currentTimeMillis());
        inst.setStatus(status);
        inst.setCreateBy("system");
        inst.setUpdateBy("system");
        return inst;
    }

    private LeaderboardMetric buildMetricAssoc(Long leaderboardId, Long metricId, int priority, String sortOrder) {
        LeaderboardMetric lm = new LeaderboardMetric();
        lm.setLeaderboardId(leaderboardId);
        lm.setMetricId(metricId);
        lm.setPriority(priority);
        lm.setSortOrder(sortOrder);
        lm.setCreateBy("system");
        lm.setUpdateBy("system");
        return lm;
    }
}
