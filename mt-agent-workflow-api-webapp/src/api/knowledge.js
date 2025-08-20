import request from './request.js'

export const knowledgeApi = {
  // 获取知识库列表
  getKnowledgeList(params) {
    return request({
      url: '/knowledge/list',
      method: 'get',
      params
    })
  },

  // 创建知识库
  createKnowledge(data) {
    return request({
      url: '/knowledge/create',
      method: 'post',
      data
    })
  },

  // 获取知识库详情
  getKnowledgeById(id) {
    return request({
      url: `/knowledge/${id}`,
      method: 'get'
    })
  },

  // 更新知识库
  updateKnowledge(data) {
    return request({
      url: '/knowledge/update',
      method: 'put',
      data
    })
  },

  // 删除知识库
  deleteKnowledge(id) {
    return request({
      url: `/knowledge/${id}`,
      method: 'delete'
    })
  },

  // 获取知识库文件列表
  getKnowledgeFiles(knowledgeId, params) {
    return request({
      url: `/knowledge/${knowledgeId}/files`,
      method: 'get',
      params
    })
  },

  // 上传文件到知识库
  uploadFile(knowledgeId, data) {
    return request({
      url: `/knowledge/${knowledgeId}/upload`,
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 删除知识库文件
  deleteFile(knowledgeId, fileId) {
    return request({
      url: `/knowledge/${knowledgeId}/files/${fileId}`,
      method: 'delete'
    })
  },

  // 获取文件文本块列表
  getFileBlocks(knowledgeId, fileId, params) {
    return request({
      url: `/knowledge/${knowledgeId}/files/${fileId}/blocks`,
      method: 'get',
      params
    })
  },

  // 更新文本块
  updateBlock(knowledgeId, fileId, blockId, data) {
    return request({
      url: `/knowledge/${knowledgeId}/files/${fileId}/blocks/${blockId}`,
      method: 'put',
      data
    })
  },

  // 获取知识关联列表
  getKnowledgeRelations(knowledgeId, params) {
    return request({
      url: `/knowledge/${knowledgeId}/relations`,
      method: 'get',
      params
    })
  },

  // 更新知识关联
  updateKnowledgeRelation(knowledgeId, relationId, data) {
    return request({
      url: `/knowledge/${knowledgeId}/relations/${relationId}`,
      method: 'put',
      data
    })
  }
}
