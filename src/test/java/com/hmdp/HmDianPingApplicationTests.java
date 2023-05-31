package com.hmdp;

import com.hmdp.controller.VoucherOrderController;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;
import static org.easymock.EasyMock.*;

@SpringBootTest
@AutoConfigureMockMvc
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private CacheClient cacheClient;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private ExecutorService es= Executors.newFixedThreadPool(500);
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }


    @Test
    void testSaveShop() throws InterruptedException {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1L,shop,10L, TimeUnit.SECONDS);
    }

    @Test
    void loadShopData(){
        //1.查询店铺信息
        List<Shop> list = shopService.list();
        //2.把店铺分组，按照typeId分组,typeId一致的放到一个集合
        Map<Long,List<Shop>> map=list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //3.1获取类型id
            Long typeId = entry.getKey();
            String key=SHOP_GEO_KEY+typeId;
            //3.2获取同类型的店铺集合
            List<Shop> value = entry.getValue();
            //3.3写入redis
            List<RedisGeoCommands.GeoLocation<String>> locations=new ArrayList<>(value.size());
            for (Shop shop : value) {
               // stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(),shop.getY())
                ));
            }

            stringRedisTemplate.opsForGeo().add(key,locations);
        }

    }
    @Test
    void testHyperLogLog(){
        String[] values=new String[1000];
        int j=0;
        for (int i = 0; i < 1000000; i++) {
            j=i%1000;
            values[j]="user_"+i;
            if(j==999){
                stringRedisTemplate.opsForHyperLogLog().add("hl2", values);
            }
        }
        Long hl2 = stringRedisTemplate.opsForHyperLogLog().size("hl2");
        System.out.println(hl2);
    }




    @Test
    void contextLoads() {

        stringRedisTemplate.opsForValue().set("key1", "你好");

    }




    @TestSubject
    private VoucherOrderController voucherOrderController ;
    private IVoucherOrderService voucherOrderService;
    @Test
    void seckill_voucher(){
        // 创建一个Mock的Result对象
        Result expectedResult = null;
        voucherOrderService=createMock(IVoucherOrderService.class);
        voucherOrderController=createMock(VoucherOrderController.class);
        //Result expectedResult = Result.fail("不能重复下单");
        // 设置Mock的IVoucherOrderService的seckillVoucher()方法的返回值
        expect(voucherOrderService.seckillVoucher(1L)).andReturn(expectedResult);
        // 激活Mock对象
        replay(voucherOrderService);

        // 调用seckillVoucher()方法
        Result result = voucherOrderController.seckillVoucher(1L);

        // 断言结果是否符合预期
        Assert.assertEquals(expectedResult, result);
        EasyMock.verify(voucherOrderService,voucherOrderController);
    }

}
