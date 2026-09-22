package com.saas.crm.common.config;

import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.customer.entity.Customer;
import com.saas.crm.customer.mapper.CustomerMapper;
import com.saas.crm.followup.entity.FollowUp;
import com.saas.crm.followup.mapper.FollowUpMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 种子数据：由 app.seed.enabled 显式开启（默认关闭），且仅当用户表为空时执行。
 * - 始终只创建主账号（账号/密码经环境变量注入）；
 * - 演示子账号与示例客户仅当 app.seed.demo-data=true（dev profile 默认）时插入，生产绝不播种演示数据。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final FollowUpMapper followUpMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.demo-data:false}")
    private boolean demoData;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:Admin@123}")
    private String adminPassword;

    @Bean
    public ApplicationRunner seedRunner() {
        return args -> {
            if (!seedEnabled) {
                return;
            }
            if (userMapper.selectCount(null) > 0) {
                return;
            }

            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRealName("主账号");
            admin.setRole(User.ROLE_ADMIN);
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("seed: admin account '{}' created", adminUsername);

            if (!demoData) {
                return;
            }

            User sales = new User();
            sales.setUsername("sales01");
            sales.setPassword(passwordEncoder.encode("Sales@123"));
            sales.setRealName("王小明");
            sales.setPhone("13800000001");
            sales.setRole(User.ROLE_MEMBER);
            sales.setStatus(1);
            userMapper.insert(sales);

            // 示例客户：覆盖全部五档意向，便于直观看到效果
            List.of(
                    newSeedCustomer("张伟", "13811112222", "宏远贸易", "采购经理", "展会", "贸易", "INTERESTED", admin.getId()),
                    newSeedCustomer("李娜", "13933334444", "蓝海科技", "运营总监", "线上咨询", "互联网", "COOPERATED", admin.getId()),
                    newSeedCustomer("王强", "13655556666", "鼎盛餐饮", "总经理", "转介绍", "餐饮", "PENDING", sales.getId()),
                    newSeedCustomer("赵敏", "13777778888", "迅捷物流", "行政主管", "广告", "物流", "NOT_INTERESTED", sales.getId()),
                    newSeedCustomer("陈杰", "13599990000", "嘉禾教育", "校长", "陌拜", "教育", "REFUSED", sales.getId())
            ).forEach(customerMapper::insert);

            Customer first = customerMapper.selectList(null).get(0);
            FollowUp followUp = new FollowUp();
            followUp.setCustomerId(first.getId());
            followUp.setContent("电话沟通了合作意向，对方对报价方案感兴趣，约了下周上门详谈。");
            followUp.setResultLevel("INTERESTED");
            followUp.setNextFollowupAt(LocalDateTime.now().plusDays(3));
            followUp.setCreatedBy(admin.getId());
            followUpMapper.insert(followUp);

            log.info("seed data created: admin={}, sales01/Sales@123 with 5 sample customers", adminUsername);
        };
    }

    private Customer newSeedCustomer(String name, String phone, String company, String position,
                                     String source, String industry, String intentLevel, Long ownerId) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setCompany(company);
        c.setPosition(position);
        c.setSource(source);
        c.setIndustry(industry);
        c.setIntentLevel(intentLevel);
        c.setOwnerId(ownerId);
        return c;
    }
}
