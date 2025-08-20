import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('../views/Layout.vue'),
    redirect: '/admin/api',
    children: [
      {
        path: 'chat-interface',
        name: 'ChatInterface',
        component: () => import('../views/ChatInterface.vue')
      },
      {
        path: 'api',
        name: 'ApiManagement',
        component: () => import('../views/ApiManagement.vue')
      },
      {
        path: 'data',
        name: 'DataManagement',
        component: () => import('../views/DataManagement.vue')
      },
      {
        path: 'extension',
        name: 'ExtensionManagement',
        component: () => import('../views/ExtensionManagement.vue')
      },
      {
        path: 'data/databases',
        name: 'DataDbList',
        component: () => import('../views/DataDbList.vue')
      },
      {
        path: 'data/new',
        name: 'DataDbWizard',
        component: () => import('../views/DataDbWizard.vue')
      },
      {
        path: 'qa',
        name: 'KnowledgeQA',
        component: () => import('../views/KnowledgeQA.vue')
      },
      {
        path: 'qa/new',
        name: 'NewKnowledgeWizard',
        component: () => import('../views/NewKnowledgeWizard.vue')
      },
      {
        path: 'qa/segmentation',
        name: 'TextSegmentationReview',
        component: () => import('../views/TextSegmentationReview.vue')
      },
      {
        path: 'qa/association',
        name: 'KnowledgeAssociationReview',
        component: () => import('../views/KnowledgeAssociationReview.vue')
      },
      {
        path: 'knowledge',
        name: 'KnowledgeManagement',
        component: () => import('../views/KnowledgeManagement.vue')
      },
      {
        path: 'knowledge/:knowledgeId/files',
        name: 'KnowledgeFiles',
        component: () => import('../views/KnowledgeFiles.vue')
      },
      {
        path: 'knowledge/:knowledgeId/files/:fileId/blocks',
        name: 'FileBlocks',
        component: () => import('../views/FileBlocks.vue')
      },
      {
        path: 'knowledge/:knowledgeId/relations',
        name: 'KnowledgeRelations',
        component: () => import('../views/KnowledgeRelations.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const isLogin = localStorage.getItem('isLogin')
  
  if (to.path === '/login') {
    if (isLogin) {
      next('/admin')
    } else {
      next()
    }
  } else {
    if (isLogin) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router