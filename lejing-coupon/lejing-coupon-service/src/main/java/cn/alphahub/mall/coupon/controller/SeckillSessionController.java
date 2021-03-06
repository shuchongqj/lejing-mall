package cn.alphahub.mall.coupon.controller;

import cn.alphahub.common.constant.HttpStatus;
import cn.alphahub.common.core.controller.BaseController;
import cn.alphahub.common.core.domain.BaseResult;
import cn.alphahub.common.core.page.PageDomain;
import cn.alphahub.common.core.page.PageResult;
import cn.alphahub.mall.coupon.domain.SeckillSession;
import cn.alphahub.mall.coupon.service.SeckillSessionService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 秒杀活动场次Controller
 *
 * @author Weasley J
 * @email 1432689025@qq.com
 * @date 2021-02-14 18:57:50
 */
@RestController
@RequestMapping("coupon/seckillsession")
public class SeckillSessionController extends BaseController {
    @Autowired
    private SeckillSessionService seckillSessionService;

    /**
     * 查询秒杀活动场次列表
     *
     * @param page           当前页码,默认第1页
     * @param rows           显示行数,默认10条
     * @param orderColumn    排序排序字段,默认不排序
     * @param isAsc          排序方式,desc或者asc
     * @param seckillSession 秒杀活动场次,查询字段选择性传入,默认为等值查询
     * @return 秒杀活动场次分页数据
     */
    @GetMapping("/list")
    public BaseResult<PageResult<SeckillSession>> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "10") Integer rows,
            @RequestParam(value = "orderColumn", defaultValue = "") String orderColumn,
            @RequestParam(value = "isAsc", defaultValue = "") String isAsc,
            SeckillSession seckillSession
    ) {
        PageDomain pageDomain = new PageDomain(page, rows, orderColumn, isAsc);
        PageResult<SeckillSession> pageResult = seckillSessionService.queryPage(pageDomain, seckillSession);
        if (ObjectUtils.isNotEmpty(pageResult.getItems())) {
            return BaseResult.ok(pageResult);
        }
        return BaseResult.fail(HttpStatus.NOT_FOUND, "查询结果为空");
    }

    /**
     * 获取秒杀活动场次详情
     *
     * @param id 秒杀活动场次主键id
     * @return 秒杀活动场次详细信息
     */
    @GetMapping("/info/{id}")
    public BaseResult<SeckillSession> info(@PathVariable("id") Long id) {
        SeckillSession seckillSession = seckillSessionService.getById(id);
        return ObjectUtils.anyNotNull(seckillSession) ? BaseResult.ok(seckillSession) : BaseResult.fail();
    }

    /**
     * 新增秒杀活动场次
     *
     * @param seckillSession 秒杀活动场次元数据
     * @return 成功返回true, 失败返回false
     */
    @PostMapping("/save")
    public BaseResult<Boolean> save(@RequestBody SeckillSession seckillSession) {
        boolean save = seckillSessionService.save(seckillSession);
        return toOperationResult(save);
    }

    /**
     * 修改秒杀活动场次
     *
     * @param seckillSession 秒杀活动场次,根据id选择性更新
     * @return 成功返回true, 失败返回false
     */
    @PutMapping("/update")
    public BaseResult<Boolean> update(@RequestBody SeckillSession seckillSession) {
        boolean update = seckillSessionService.updateById(seckillSession);
        return toOperationResult(update);
    }

    /**
     * 批量删除秒杀活动场次
     *
     * @param ids 秒杀活动场次id集合
     * @return 成功返回true, 失败返回false
     */
    @DeleteMapping("/delete/{ids}")
    public BaseResult<Boolean> delete(@PathVariable Long[] ids) {
        boolean delete = seckillSessionService.removeByIds(Arrays.asList(ids));
        return toOperationResult(delete);
    }
}
