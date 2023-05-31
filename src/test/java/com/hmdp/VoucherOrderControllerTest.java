package com.hmdp;

/**
 * @author zhusiyuan
 * @date 2023/4/17
 * @apiNote
 */
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import
        org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
/**
 * 使⽤前提：
 * 1.有糊涂⼯具类的依赖
 * 2.登陆功能会返回验证码，⽤户不存在会⾃动注册，且⽤phone字段来进⾏登陆。
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoucherOrderControllerTest {
    @Resource
    private MockMvc mockMvc;
    @Resource
    private IUserService userService;
    @Resource
    private ObjectMapper mapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public static int USER_NUMBER=1000;

    @Test
    @SneakyThrows
    @DisplayName("创建1000个⽤户到数据库")
    void createUser() {
        List<String> phoneList = new ArrayList<>();
        for (int i = 0; i < USER_NUMBER; i++) {
            String phone = String.format("131%s", RandomUtil.randomInt(10000000,
                    99999999));
            phoneList.add(phone);
        }

        ExecutorService executorService =
                ThreadUtil.newExecutor(phoneList.size());
        CountDownLatch countDownLatch = new CountDownLatch(phoneList.size());
        phoneList.forEach(phone -> {
            executorService.execute(() -> {
                try {
                    codeAndLogin(phone);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        countDownLatch.await();
        executorService.shutdown();
        System.out.println("创建: " + phoneList.size() + " 个⽤户成功");
    }
    @Test
    @SneakyThrows
    @DisplayName("登录1000个⽤户，并输出到⽂件中")
    void login() {
        List<String> phoneList = userService.lambdaQuery()
                .select(User::getPhone)
                .last("limit "+USER_NUMBER)
                .list().stream().map(User::getPhone).collect(Collectors.toList());
        ExecutorService executorService =
                ThreadUtil.newExecutor(phoneList.size());
        List<String> tokenList = new CopyOnWriteArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(phoneList.size());
        phoneList.forEach(phone -> {
            executorService.execute(() -> {
                try {
                    String token = codeAndLogin(phone);
                    tokenList.add(token);
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
        countDownLatch.await();
        executorService.shutdown();
        Assert.isTrue(tokenList.size() == phoneList.size());
        writeToTxt(tokenList, "/tokens.txt");
        System.out.println("写⼊完成！");
    }
    /**
     * 获取验证码，且登陆
     * @param phone
     * @return token
     */
    private String codeAndLogin(String phone) throws Exception {
// 验证码
        String codeJson = mockMvc.perform(MockMvcRequestBuilders
                        .post("/user/code")
                        .queryParam("phone", phone))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        Result result = mapper.readerFor(Result.class).readValue(codeJson);
        Assert.isTrue(result.getSuccess(), String.format("获取“%s”⼿机号的验证码失败", phone));
//String code = result.getData().toString();
                String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY +
                        phone);
        LoginFormDTO formDTO = new LoginFormDTO();
        formDTO.setCode(code);
        formDTO.setPhone(phone);
        String json = mapper.writeValueAsString(formDTO);
// token
        String tokenJson = mockMvc.perform(MockMvcRequestBuilders
                        .post("/user/login")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        result = mapper.readerFor(Result.class).readValue(tokenJson);
        Assert.isTrue(result.getSuccess(), String.format("获取“%s”⼿机号的token失败,json为“%s”", phone, json));
                String token = result.getData().toString();
        return token;
    }
    //⽣成token⽂件
    private static void writeToTxt(List<String> list, String suffixPath) throws
            Exception {
// 1. 创建⽂件
        File file = new File("C:\\Users\\12402\\Desktop\\123" + suffixPath);
        if (!file.exists()) {
            file.createNewFile();
        }
// 2. 输出
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new
                FileOutputStream(file), StandardCharsets.UTF_8));
        for (String content : list) {
            bw.write(content);
            bw.newLine();
        }
        bw.close();
        System.out.println("写⼊完成！");
    }
}