package cn.alphahub.mall.product.service.impl;

import cn.alphahub.common.constant.ProductConstant;
import cn.alphahub.common.core.page.PageDomain;
import cn.alphahub.common.core.page.PageResult;
import cn.alphahub.mall.product.domain.Attr;
import cn.alphahub.mall.product.domain.AttrAttrgroupRelation;
import cn.alphahub.mall.product.domain.AttrGroup;
import cn.alphahub.mall.product.domain.Category;
import cn.alphahub.mall.product.mapper.AttrAttrgroupRelationMapper;
import cn.alphahub.mall.product.mapper.AttrGroupMapper;
import cn.alphahub.mall.product.mapper.AttrMapper;
import cn.alphahub.mall.product.mapper.CategoryMapper;
import cn.alphahub.mall.product.service.AttrService;
import cn.alphahub.mall.product.service.CategoryService;
import cn.alphahub.mall.product.vo.AttrGroupVO;
import cn.alphahub.mall.product.vo.AttrRespVO;
import cn.alphahub.mall.product.vo.AttrVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品属性Service业务层处理
 *
 * @author Weasley J
 * @email 1432689025@qq.com
 * @date 2021-02-07 22:46:24
 */
@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrMapper, Attr> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationMapper attrAttrgroupRelationMapper;
    @Resource
    private AttrGroupMapper attrGroupMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private CategoryService categoryService;

    /**
     * 查询商品属性分页列表
     *
     * @param pageDomain 分页数据
     * @param attr       分页对象
     * @return 商品属性分页数据
     */
    @Override
    public PageResult<Attr> queryPage(PageDomain pageDomain, Attr attr) {
        pageDomain.startPage();
        QueryWrapper<Attr> wrapper = new QueryWrapper<>(attr);
        return getPageResult(wrapper);
    }

    @Override
    public PageResult<AttrRespVO> queryPage(PageDomain pageDomain, Attr attr, String key, Long catelogId, String attrType) {

        QueryWrapper<Attr> wrapper = new QueryWrapper<>(attr);

        int val = StringUtils.equalsIgnoreCase(attrType, "base")
                ? ProductConstant.AttrEnum.BASE.getCode()
                : ProductConstant.AttrEnum.SALE.getCode();
        wrapper.lambda().eq(Attr::getAttrType, val);

        if (!Objects.equals(catelogId, 0L)) {
            wrapper.lambda().eq(Attr::getCatelogId, catelogId);
        }

        if (StringUtils.isNotBlank(key)) {
            wrapper.and(attrQueryWrapper -> {
                attrQueryWrapper.lambda()
                        .eq(Attr::getAttrId, key)
                        .or()
                        .like(Attr::getAttrName, key);
            });
        }

        // 处理数据
        pageDomain.startPage();
        PageResult<Attr> pageResult = this.getPageResult(wrapper);
        List<AttrRespVO> respVos = pageResult.getItems().stream().map(attr2 -> {
            AttrRespVO respVo = new AttrRespVO();
            BeanUtils.copyProperties(attr2, respVo);

            // 设置分类和分组名
            QueryWrapper<AttrAttrgroupRelation> queryWrapper = new QueryWrapper<>();
            if (StringUtils.isBlank(attrType) || StringUtils.equalsIgnoreCase(attrType, "base")) {
                queryWrapper.lambda().eq(AttrAttrgroupRelation::getAttrId, respVo.getAttrId());
                List<AttrAttrgroupRelation> attrgroupRelations = this.attrAttrgroupRelationMapper.selectList(queryWrapper);
                if (CollectionUtils.isNotEmpty(attrgroupRelations)) {
                    Long attrGroupId = attrgroupRelations.get(0).getAttrGroupId();
                    AttrGroup attrGroup = attrGroupMapper.selectById(attrGroupId);
                    respVo.setGroupName(attrGroup.getAttrGroupName());
                }
            }
            Category category = categoryMapper.selectById(attr2.getCatelogId());
            if (Objects.nonNull(category)) {
                respVo.setCatelogName(category.getName());
            }
            return respVo;
        }).collect(Collectors.toList());

        // 重新构造一个分页对象
        return PageResult.<AttrRespVO>builder()
                .totalCount(pageResult.getTotalCount())
                .totalPage(pageResult.getTotalPage())
                .items(respVos)
                .build();
    }

    private PageResult<Attr> getPageResult(Wrapper<Attr> wrapper) {
        List<Attr> list = this.list(wrapper);
        PageInfo<Attr> pageInfo = new PageInfo<>(list);
        return PageResult.<Attr>builder()
                .totalCount(pageInfo.getTotal())
                .totalPage((long) pageInfo.getPages())
                .items(pageInfo.getList())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAttr(AttrVO attrVo) {
        Attr attr = new Attr();
        BeanUtils.copyProperties(attrVo, attr);
        // 保存基本数据
        boolean save = this.save(attr);
        //保存关联关系
        if (Objects.equals(attrVo.getAttrType(), ProductConstant.AttrEnum.BASE.getCode())) {
            AttrAttrgroupRelation relation = new AttrAttrgroupRelation();
            relation.setAttrGroupId(attrVo.getAttrGroupId());
            relation.setAttrId(attr.getAttrId());
            if (ObjectUtils.isNotEmpty(relation)) {
                return attrAttrgroupRelationMapper.insert(relation) >= 1;
            }
        }
        return save;
    }


    /**
     * 修改商品属性
     *
     * @param attrVo 商品属性-视图对象
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAttrById(AttrVO attrVo) {
        Attr attr = new Attr();
        BeanUtils.copyProperties(attrVo, attr);
        // 保存基本数据
        boolean update = this.updateById(attr);

        //基本类型才修改分组
        if (Objects.equals(attr.getAttrType(), ProductConstant.AttrEnum.BASE.getCode())) {
            AttrAttrgroupRelation relation = new AttrAttrgroupRelation();
            relation.setAttrId(attr.getAttrId());
            relation.setAttrGroupId(attrVo.getAttrGroupId());

            if (ObjectUtils.isNotEmpty(relation)) {
                QueryWrapper<AttrAttrgroupRelation> queryWrapper = new QueryWrapper<>();
                Integer count = attrAttrgroupRelationMapper.selectCount(
                        queryWrapper.lambda().eq(AttrAttrgroupRelation::getAttrId, attr.getAttrId())
                );
                UpdateWrapper<AttrAttrgroupRelation> wrapper = new UpdateWrapper<>();
                int insetIfNotExist = count > 0 ?
                        attrAttrgroupRelationMapper.update(
                                relation, wrapper.lambda().eq(AttrAttrgroupRelation::getAttrId, attr.getAttrId())
                        ) : attrAttrgroupRelationMapper.insert(relation);
                return insetIfNotExist >= 1;
            }
        }
        return update;
    }

    /**
     * 获取商品属性详情
     *
     * @param attrId 商品属性主键id
     * @return 商品属性详细信息
     */
    @Override
    public AttrRespVO getAttrInfoById(Long attrId) {
        AttrRespVO attrRespVo = new AttrRespVO();
        Attr attr = this.getById(attrId);
        BeanUtils.copyProperties(attr, attrRespVo);
        QueryWrapper<AttrAttrgroupRelation> wrapper = new QueryWrapper<>();

        AttrAttrgroupRelation attrgroupRelation = null;
        if (Objects.equals(attr.getAttrType(), ProductConstant.AttrEnum.BASE.getCode())) {
            //设置分组信息
            attrgroupRelation = attrAttrgroupRelationMapper.selectOne(
                    wrapper.lambda().eq(AttrAttrgroupRelation::getAttrId, attrId)
            );
        }

        AttrGroup attrGroup = attrGroupMapper.selectById(attrId);
        Long[] catelogFullPath = categoryService.getCatelogFullPath(attr.getCatelogId());
        Category category = categoryMapper.selectById(attr.getCatelogId());
        // 组装数据
        attrRespVo.setCatelogPath(catelogFullPath);
        attrRespVo.setCatelogName(Objects.nonNull(category) ? category.getName() : null);
        attrRespVo.setAttrGroupId(Objects.nonNull(attrgroupRelation) ? attrgroupRelation.getAttrGroupId() : null);
        attrRespVo.setGroupName(Objects.nonNull(attrGroup) ? attrGroup.getAttrGroupName() : null);
        return attrRespVo;
    }

    @Override
    public List<Attr> getAttrRelations(Long attrGroupId) {
        QueryWrapper<AttrAttrgroupRelation> wrapper = new QueryWrapper<>();
        List<AttrAttrgroupRelation> relations = attrAttrgroupRelationMapper.selectList(wrapper.lambda().eq(AttrAttrgroupRelation::getAttrGroupId, attrGroupId));
        List<Long> ids = relations.stream().map(AttrAttrgroupRelation::getAttrId).collect(Collectors.toList());
        return CollectionUtils.isEmpty(ids) ? null : this.listByIds(ids);
    }


    @Override
    public Integer removeRelations(List<AttrGroupVO> attrGroupVOList) {
        List<AttrAttrgroupRelation> relations = attrGroupVOList.stream().map(attrGroupVO -> {
            AttrAttrgroupRelation build = AttrAttrgroupRelation.builder().build();
            BeanUtils.copyProperties(attrGroupVO, build);
            return build;
        }).collect(Collectors.toList());

        return CollectionUtils.isEmpty(relations) ? 0 : this.attrAttrgroupRelationMapper.deleteBatchRelation(relations);
    }

    @Override
    public PageResult<Attr> getAttrNoRelations(PageDomain pageDomain, Long attrGroupId, String key) {

        // 1 当前分组只能关联自己所属分类里面的所有属性
        AttrGroup attrGroup = attrGroupMapper.selectById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        // 2 当前分组只能关联别的分组没被引用的属性
        // 2.1 当前分类下的其他分组id
        QueryWrapper<AttrGroup> wrapper = new QueryWrapper<>();
        List<AttrGroup> attrGroups = attrGroupMapper.selectList(wrapper.lambda().eq(AttrGroup::getCatelogId, catelogId));
        List<Long> attrGroupIds = attrGroups.stream().map(AttrGroup::getAttrGroupId).collect(Collectors.toList());
        // 2.2 这些分组下关联的属性id
        QueryWrapper<AttrAttrgroupRelation> wrapper1 = new QueryWrapper<>();
        if (CollectionUtils.isNotEmpty(attrGroupIds)) {
            wrapper1.lambda().in(AttrAttrgroupRelation::getAttrGroupId, attrGroupIds);
        }
        List<AttrAttrgroupRelation> relations = attrAttrgroupRelationMapper.selectList(wrapper1);
        List<Long> attrIds = relations.stream().map(AttrAttrgroupRelation::getAttrId).collect(Collectors.toList());
        // 2.3 从当前分类的所有属性中移除2.2的属性id
        QueryWrapper<Attr> wrapper2 = new QueryWrapper<>();
        wrapper2.lambda().eq(Attr::getCatelogId, catelogId).eq(Attr::getAttrType, ProductConstant.AttrEnum.BASE.getCode());
        if (CollectionUtils.isNotEmpty(attrIds)) {
            wrapper2.lambda().notIn(Attr::getAttrId, attrIds);
        }
        if (StringUtils.isNotBlank(key)) {
            wrapper2.lambda().and(lambdaWrapper -> lambdaWrapper.eq(Attr::getAttrId, key).or().like(Attr::getAttrName, key));
        }
        // 构造一个Attr的分页对象
        PageResult<Attr> pageResult = new PageResult<>();
        //开始分页
        pageResult.startPage(pageDomain);
        //封装分页数据并返回
        List<Attr> attrs = this.list(wrapper2);
        return pageResult.getPage(attrs);
    }
}
