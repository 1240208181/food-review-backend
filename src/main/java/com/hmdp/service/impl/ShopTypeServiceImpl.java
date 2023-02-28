package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopList() {
        String key=CACHE_SHOP_TYPE_KEY;
        //1.从redis查询商铺缓存
        String jsonArray = stringRedisTemplate.opsForValue().get(key);

        //如果存在返回
        if(StrUtil.isNotBlank(jsonArray)){
            List<ShopType> jsonList = JSONUtil.toList(jsonArray, ShopType.class);
            return Result.ok(jsonList);
        }
        //4.不存在，查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        //5.不存在返回错误
        if(shopTypes==null){
            return Result.fail("未查询到信息");
        }
        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}
