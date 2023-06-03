package com.hmdp.service.impl;

import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.StringJoiner;


@SpringBootTest
@DisplayName("点评模块")
class BlogServiceImplTest {

    @Resource
    private IBlogService blogService;

    private Blog blog;

    @BeforeEach
    void before () {
        blog = blogService.lambdaQuery().last(" limit 1 ").one();
    }

    @ParameterizedTest(name = "{index}: 标题长度{0}, 预计结果{1}")
    @CsvSource({
            "0,false",
            "1,true",
            "256,false"
    })
    @DisplayName("标题长度测试")
    void testTitle (int length, boolean expect) {
        // 初始化参数
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("a");
        }
        blog.setId(null);
        blog.setTitle(stringBuilder.toString());
        if (length == 0) {
            blog.setTitle(null);
        }

        // 测试保存
        boolean save = false;
        try {
            save = blogService.save(blog);
            Assert.isTrue(save == expect, "与预期结果不符");
        } catch (Exception e) {
            Assert.isTrue(expect == false, "与预期结果不符");
        } finally {
            if (Boolean.TRUE.equals(save)) {
                // 删除测试数据
                boolean remove = blogService.removeById(blog.getId());
                Assert.isTrue(remove, "删除测试数据错误");
            }
        }
    }


    @ParameterizedTest(name = "{index}: 内容长度{0}, 预计结果{1}")
    @CsvSource({
            "100,true",
            "0,false",
            "2049,false"
    })
    @DisplayName("内容长度测试")
    void testContent (int length, boolean expect) {
        // 初始化参数
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("a");
        }
        blog.setId(null);
        blog.setContent(stringBuilder.toString());
        if (length == 0) {
            blog.setContent(null);
        }

        // 测试保存
        boolean save = false;
        try {
            save = blogService.save(blog);
            Assert.isTrue(save == expect, "与预期结果不符");
        } catch (Exception e) {
            Assert.isTrue(expect == false, "与预期结果不符");
        } finally {
            if (Boolean.TRUE.equals(save)) {
                // 删除测试数据
                boolean remove = blogService.removeById(blog.getId());
                Assert.isTrue(remove, "删除测试数据错误");
            }
        }
    }


    @ParameterizedTest(name = "{index}: 图片数量{0}, 预计结果{1}")
    @CsvSource({
            "0,true",
            "3,true",
            "10,false"
    })
    @DisplayName("图片数量测试")
    void testImageCount (int count, boolean expect) {
        // 初始化参数
        StringJoiner stringJoiner = new StringJoiner(",");
        for (int i = 0; i < count; i++) {
            stringJoiner.add("/imgs/blogs/2/6/b0756279-65da-4f2d-b62a-33f74b06454a.jpg");
        }
        blog.setId(null);
        blog.setImages(stringJoiner.toString());

        // 测试保存
        boolean save = false;
        try {
            save = blogService.save(blog);
            Assert.isTrue(save == expect, "与预期结果不符");
        } catch (Exception e) {
            Assert.isTrue(expect == false && !save, "与预期结果不符");
        } finally {
            if (Boolean.TRUE.equals(save)) {
                // 删除测试数据
                boolean remove = blogService.removeById(blog.getId());
                Assert.isTrue(remove, "删除测试数据错误");
            }
        }
    }

    @Test
    @DisplayName("上传时商户为空")
    void testNullShopId () {
        boolean expect = false;

        // 测试保存
        boolean save = false;
        try {
            blog.setShopId(null);
            save = blogService.save(blog);
            Assert.isTrue(save == expect, "与预期结果不符");
        } catch (Exception e) {
            Assert.isTrue(expect == false, "与预期结果不符");
        } finally {
            if (Boolean.TRUE.equals(save)) {
                // 删除测试数据
                boolean remove = blogService.removeById(blog.getId());
                Assert.isTrue(remove, "删除测试数据错误");
            }
        }
    }

    @Test
    @DisplayName("上传时商户不为空")
    void testNotNullShopId () {
        boolean expect = true;

        // 测试保存
        boolean save = false;
        try {
            blog.setShopId(1L);
            save = blogService.save(blog);
            Assert.isTrue(save == expect, "与预期结果不符");
        } catch (Exception e) {
            Assert.isTrue(expect == false, "与预期结果不符");
        } finally {
            if (Boolean.TRUE.equals(save)) {
                // 删除测试数据
                boolean remove = blogService.removeById(blog.getId());
                Assert.isTrue(remove, "删除测试数据错误");
            }
        }
    }


    @Test
    @DisplayName("删除点评")
    void testDelete() {
        // 1.插入一条测试数据
        blog.setId(null);
        blog.setTitle("测试 " + blog.getTitle());
        boolean save = blogService.save(blog);
        Assert.isTrue(save, "测试数据保存错误");

        // 2.测试删除点评
        boolean remove = blogService.removeById(blog.getId());
        Assert.isTrue(remove, "删除数据错误");

        // 3.检查数据库
        Blog byId = blogService.getById(blog.getId());
        Assert.isTrue(byId == null, "未知异常");
    }

    @Test
    @DisplayName("修改点评")
    void testUpdate() {
        // 1.获取一条测试数据
        blog.setId(null);
        blog.setTitle("测试 " + blog.getName());
        boolean save = blogService.save(blog);
        Assert.isTrue(save, "测试数据保存错误");

        // 2.测试修改
        String title = String.valueOf(Math.random() * 1000);
        blog.setTitle(title);
        boolean update = blogService.updateById(blog);
        Assert.isTrue(update, "修改数据错误");

        // 3.检查数据库数据是否正确
        Blog newBlog = blogService.getById(blog.getId());
        Assert.isTrue(Objects.equals(newBlog.getId(), blog.getId()), "id错误");
        Assert.isTrue(Objects.equals(newBlog.getTitle(), blog.getTitle()), "title错误");

        // 4.删除测试数据
        boolean remove = blogService.removeById(blog.getId());
        Assert.isTrue(remove, "删除测试数据错误");
    }

    @Test
    @DisplayName("sql注入")
    void sqlInject() {
        blog.setId(null);
        String title = "','aaaaaaa";
        blog.setTitle(title);
        boolean save = false;
        try {
            save = blogService.save(blog);
        } catch (Exception ignore) {
        }
        if (save) {
            blogService.removeById(blog.getId());
        }
        Assert.isTrue(save == true, "存在sql注入");
    }



}