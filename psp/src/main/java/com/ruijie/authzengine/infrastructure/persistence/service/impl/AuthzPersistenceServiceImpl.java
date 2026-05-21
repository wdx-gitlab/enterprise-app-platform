package com.ruijie.authzengine.infrastructure.persistence.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * authz-engine 专属 MyBatis-Plus 持久化基类，统一绑定专属事务管理器。
 *
 * @param <M> Mapper 类型
 * @param <T> 实体类型
 */
@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
public abstract class AuthzPersistenceServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean save(T entity) {
		return super.save(entity);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean saveBatch(Collection<T> entityList) {
		return super.saveBatch(entityList, DEFAULT_BATCH_SIZE);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean saveBatch(Collection<T> entityList, int batchSize) {
		return super.saveBatch(entityList, batchSize);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean saveOrUpdate(T entity) {
		return super.saveOrUpdate(entity);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper) {
		return super.saveOrUpdate(entity, updateWrapper);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean saveOrUpdateBatch(Collection<T> entityList) {
		return super.saveOrUpdateBatch(entityList, DEFAULT_BATCH_SIZE);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
		return super.saveOrUpdateBatch(entityList, batchSize);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean removeById(Serializable id) {
		return super.removeById(id);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean removeByMap(Map<String, Object> columnMap) {
		return super.removeByMap(columnMap);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean remove(Wrapper<T> queryWrapper) {
		return super.remove(queryWrapper);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean removeByIds(Collection<? extends Serializable> idList) {
		return super.removeByIds(idList);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean updateById(T entity) {
		return super.updateById(entity);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean update(Wrapper<T> updateWrapper) {
		return super.update(updateWrapper);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean update(T entity, Wrapper<T> updateWrapper) {
		return super.update(entity, updateWrapper);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean updateBatchById(Collection<T> entityList) {
		return super.updateBatchById(entityList, DEFAULT_BATCH_SIZE);
	}

	@Override
	@Transactional(transactionManager = "authzTransactionManager", rollbackFor = Exception.class)
	public boolean updateBatchById(Collection<T> entityList, int batchSize) {
		return super.updateBatchById(entityList, batchSize);
	}
}