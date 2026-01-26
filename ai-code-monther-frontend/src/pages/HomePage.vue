<template>
  <div id="homePage">
    <!-- 星星雨画布 -->
    <canvas ref="starsCanvas" class="stars-canvas"></canvas>
    <!-- 粒子画布 -->
    <canvas ref="particlesCanvas" class="particles-canvas"></canvas>

    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <img class="hero-logo" src="@/assets/logo-warm.svg" alt="logo" />
        <h1 class="hero-title">Buling 秒搭</h1>
        <p class="hero-description">一句话轻松实现你的灵光一闪</p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section">
        <a-textarea
          v-model:value="userPrompt"
          :placeholder="placeholderText"
          :rows="6"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="input-actions">
          <a-button type="primary" class="create-btn" @click="createApp" :loading="creating">
            <template #icon>
              <span>↑</span>
            </template>
          </a-button>
        </div>
      </div>

      <!-- 快捷按钮 -->
      <div class="quick-actions">
        <div
          v-for="(prompt, index) in creativePrompts"
          :key="index"
          class="quick-card"
          @click="setPrompt(prompt.text)"
        >
          <div class="quick-title">{{ prompt.title }}</div>
        </div>
      </div>

      <!-- 作品和案例合并卡片 -->
      <div class="works-card">
        <a-tabs v-model:activeKey="activeTab" class="works-tabs">
          <a-tab-pane key="my" tab="我的作品">
            <div class="tab-content">
              <div class="app-grid">
                <AppCard
                  v-for="app in myApps"
                  :key="app.id"
                  :app="app"
                  @view-chat="viewChat"
                  @view-work="viewWork"
                />
              </div>
              <div v-if="myApps.length === 0" class="empty-state">
                <div class="empty-icon">🎨</div>
                <div class="empty-text">还没有作品，快去创建一个吧！</div>
              </div>
              <div v-if="myApps.length > 0" class="pagination-wrapper">
                <a-pagination
                  v-model:current="myAppsPage.current"
                  v-model:page-size="myAppsPage.pageSize"
                  :total="myAppsPage.total"
                  :show-size-changer="false"
                  :show-total="(total: number) => `共 ${total} 个应用`"
                  @change="loadMyApps"
                />
              </div>
            </div>
          </a-tab-pane>
          <a-tab-pane key="featured" tab="精选案例">
            <div class="tab-content">
              <div class="featured-grid">
                <AppCard
                  v-for="app in featuredApps"
                  :key="app.id"
                  :app="app"
                  :featured="true"
                  @view-chat="viewChat"
                  @view-work="viewWork"
                />
              </div>
              <div v-if="featuredApps.length === 0" class="empty-state">
                <div class="empty-icon">✨</div>
                <div class="empty-text">精选案例即将上线</div>
              </div>
              <div v-if="featuredApps.length > 0" class="pagination-wrapper">
                <a-pagination
                  v-model:current="featuredAppsPage.current"
                  v-model:page-size="featuredAppsPage.pageSize"
                  :total="featuredAppsPage.total"
                  :show-size-changer="false"
                  :show-total="(total: number) => `共 ${total} 个案例`"
                  @change="loadFeaturedApps"
                />
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 当前激活的Tab
const activeTab = ref('my')

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// Placeholder打字机效果
const placeholderText = ref('')
const placeholderIndex = ref(0)
const charIndex = ref(0)
const isDeleting = ref(false)
const typingInterval = ref<number | null>(null)

// 画布引用
const starsCanvas = ref<HTMLCanvasElement | null>(null)
const particlesCanvas = ref<HTMLCanvasElement | null>(null)
const starsCtx = ref<CanvasRenderingContext2D | null>(null)
const particlesCtx = ref<CanvasRenderingContext2D | null>(null)

// 星星雨相关
const stars = ref<any[]>([])
const starsAnimationId = ref<number | null>(null)

// 点击粒子相关
const particles = ref<any[]>([])
const particlesAnimationId = ref<number | null>(null)

const placeholders = [
  '帮我创建一个个人博客网站',
  '设计一个极简风格的企业官网',
  '制作一个在线商城，支持购物车功能',
  '创建一个精美的作品集展示网站',
]

// 创意快捷按钮
const creativePrompts = [
  {
    icon: '🌆',
    title: '赛博朋克',
    desc: '霓虹灯与未来都市',
    text: '打造一个赛博朋克风格的网站，采用霓虹灯、全息投影、故障艺术效果。深色背景搭配紫色、粉色、青色霓虹光效，包含动态粒子背景、故障文字效果、全息卡片展示，要有强烈的未来科技感和反乌托邦美学。',
  },
  {
    icon: '🎭',
    title: '暗黑童话',
    desc: '神秘哥特美学',
    text: '设计一个暗黑童话风格的网站，采用哥特美学，包含荆棘、玫瑰、乌鸦、古堡等元素。深紫色、深红色、黑色为主色调，配合星空、萤火虫效果，神秘而浪漫，要有幽灵般的悬停动画和魔法粒子特效。',
  },
  {
    icon: '🏯',
    title: '国潮古风',
    desc: '水墨丹青东方美学',
    text: '创建一个国潮古风网站，采用水墨画风格，融合现代设计元素。包含云纹、山水、古典纹样等传统元素，使用水墨渐变、毛笔字体、古典配色（朱砂、靛蓝、墨黑），要有留白艺术意境，配合优雅的纸张纹理背景。',
  },
  {
    icon: '🌌',
    title: '星际漫游',
    desc: '宇宙浩瀚探索',
    text: '创建一个星际漫游主题网站，采用宇宙星空元素，包含行星、星云、黑洞、宇航员等。深蓝、紫色、黑色为主，配合星芒、流星、星轨动画效果，3D星球展示，要有浩瀚宇宙的神秘感和探索未知的浪漫主义色彩。',
  },
]

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 打字机效果
const startTyping = () => {
  if (typingInterval.value) {
    clearInterval(typingInterval.value)
  }

  const type = () => {
    const currentPlaceholder = placeholders[placeholderIndex.value]

    if (isDeleting.value) {
      placeholderText.value = currentPlaceholder.substring(0, charIndex.value - 1)
      charIndex.value--
    } else {
      placeholderText.value = currentPlaceholder.substring(0, charIndex.value + 1)
      charIndex.value++
    }

    let typeSpeed = 100

    if (isDeleting.value) {
      typeSpeed = 50
    }

    if (!isDeleting.value && charIndex.value === currentPlaceholder.length) {
      typeSpeed = 2000
      isDeleting.value = true
    } else if (isDeleting.value && charIndex.value === 0) {
      isDeleting.value = false
      placeholderIndex.value = (placeholderIndex.value + 1) % placeholders.length
      typeSpeed = 500
    }

    typingInterval.value = setTimeout(type, typeSpeed)
  }

  type()
}

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
  startTyping()

  // 初始化星星雨特效
  nextTick(() => {
    initStarsCanvas()
    initParticlesCanvas()
  })

  // 窗口大小改变监听
  window.addEventListener('resize', handleResize)

  // 鼠标跟随光效
  const handleMouseMove = (e: MouseEvent) => {
    const { clientX, clientY } = e
    const { innerWidth, innerHeight } = window
    const x = (clientX / innerWidth) * 100
    const y = (clientY / innerHeight) * 100

    document.documentElement.style.setProperty('--mouse-x', `${x}%`)
    document.documentElement.style.setProperty('--mouse-y', `${y}%`)
  }

  document.addEventListener('mousemove', handleMouseMove)

  // 清理事件监听器
  return () => {
    document.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('resize', handleResize)
    if (typingInterval.value) {
      clearInterval(typingInterval.value)
    }
    if (starsAnimationId.value) {
      cancelAnimationFrame(starsAnimationId.value)
    }
    if (particlesAnimationId.value) {
      cancelAnimationFrame(particlesAnimationId.value)
    }
  }
})

onUnmounted(() => {
  if (typingInterval.value) {
    clearInterval(typingInterval.value)
  }
  if (starsAnimationId.value) {
    cancelAnimationFrame(starsAnimationId.value)
  }
  if (particlesAnimationId.value) {
    cancelAnimationFrame(particlesAnimationId.value)
  }
  document.removeEventListener('click', handleClickParticle)
})

// ========== 星星雨特效 ==========
const initStarsCanvas = () => {
  if (!starsCanvas.value) return

  starsCanvas.value.width = window.innerWidth
  starsCanvas.value.height = window.innerHeight
  starsCtx.value = starsCanvas.value.getContext('2d')

  // 创建星星
  const starCount = 80
  for (let i = 0; i < starCount; i++) {
    stars.value.push({
      x: Math.random() * starsCanvas.value.width,
      y: Math.random() * starsCanvas.value.height,
      size: Math.random() * 3 + 1,
      speed: Math.random() * 2 + 1,
      opacity: Math.random() * 0.5 + 0.3,
      twinkle: Math.random() * 0.02 + 0.01,
    })
  }

  animateStars()
}

const animateStars = () => {
  if (!starsCtx.value || !starsCanvas.value) return

  const ctx = starsCtx.value
  const canvas = starsCanvas.value

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  stars.value.forEach((star) => {
    // 更新位置
    star.y += star.speed
    star.opacity += star.twinkle

    // 闪烁效果
    if (star.opacity > 1 || star.opacity < 0.3) {
      star.twinkle *= -1
    }

    // 超出边界重置
    if (star.y > canvas.height) {
      star.y = -star.size
      star.x = Math.random() * canvas.width
    }

    // 绘制星星
    ctx.beginPath()
    ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255, 255, 255, ${star.opacity})`
    ctx.fill()

    // 添加光晕效果
    if (star.size > 2) {
      const gradient = ctx.createRadialGradient(star.x, star.y, 0, star.x, star.y, star.size * 2)
      gradient.addColorStop(0, `rgba(255, 255, 255, ${star.opacity * 0.3})`)
      gradient.addColorStop(1, 'rgba(255, 255, 255, 0)')
      ctx.beginPath()
      ctx.arc(star.x, star.y, star.size * 2, 0, Math.PI * 2)
      ctx.fillStyle = gradient
      ctx.fill()
    }
  })

  starsAnimationId.value = requestAnimationFrame(animateStars)
}

// ========== 点击粒子特效 ==========
const initParticlesCanvas = () => {
  if (!particlesCanvas.value) return

  particlesCanvas.value.width = window.innerWidth
  particlesCanvas.value.height = window.innerHeight
  particlesCtx.value = particlesCanvas.value.getContext('2d')

  document.addEventListener('click', handleClickParticle)
  animateParticles()
}

const handleClickParticle = (e: MouseEvent) => {
  if (!particlesCanvas.value) return

  const particleCount = 8
  const colors = [
    'rgba(255, 184, 108, 0.8)',
    'rgba(255, 138, 91, 0.8)',
    'rgba(255, 111, 97, 0.8)',
    'rgba(255, 196, 136, 0.8)',
  ]

  for (let i = 0; i < particleCount; i++) {
    const angle = ((Math.PI * 2) / particleCount) * i
    const speed = Math.random() * 2 + 1

    particles.value.push({
      x: e.clientX,
      y: e.clientY,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      size: Math.random() * 2 + 1,
      color: colors[Math.floor(Math.random() * colors.length)],
      opacity: 0.6,
      gravity: 0.1,
      life: 1,
    })
  }
}

const animateParticles = () => {
  if (!particlesCtx.value || !particlesCanvas.value) return

  const ctx = particlesCtx.value
  const canvas = particlesCanvas.value

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 更新和绘制粒子
  for (let i = particles.value.length - 1; i >= 0; i--) {
    const p = particles.value[i]

    // 更新位置
    p.x += p.vx
    p.y += p.vy
    p.vy += p.gravity
    p.life -= 0.02
    p.opacity = p.life

    // 移除消失的粒子
    if (p.life <= 0) {
      particles.value.splice(i, 1)
      continue
    }

    // 绘制粒子
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size * p.life, 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.globalAlpha = p.opacity
    ctx.fill()

    // 添加微弱光晕
    if (p.size * p.life > 1) {
      const gradient = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.size * 1.5)
      gradient.addColorStop(0, p.color)
      gradient.addColorStop(1, 'rgba(255, 255, 255, 0)')
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size * 1.5 * p.life, 0, Math.PI * 2)
      ctx.fillStyle = gradient
      ctx.globalAlpha = p.opacity * 0.3
      ctx.fill()
    }
  }

  ctx.globalAlpha = 1
  particlesAnimationId.value = requestAnimationFrame(animateParticles)
}

// 窗口大小改变时重新调整画布
const handleResize = () => {
  if (starsCanvas.value) {
    starsCanvas.value.width = window.innerWidth
    starsCanvas.value.height = window.innerHeight
  }
  if (particlesCanvas.value) {
    particlesCanvas.value.width = window.innerWidth
    particlesCanvas.value.height = window.innerHeight
  }
}
</script>
<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background:
    linear-gradient(180deg, #fff7f2 0%, #fff3ec 10%, #ffece3 22%, #ffe6db 100%),
    radial-gradient(circle at 20% 80%, rgba(255, 184, 108, 0.18) 0%, transparent 52%),
    radial-gradient(circle at 80% 20%, rgba(255, 111, 97, 0.14) 0%, transparent 52%),
    radial-gradient(circle at 40% 40%, rgba(255, 138, 91, 0.1) 0%, transparent 52%);
  position: relative;
  overflow: hidden;
}

/* 星星雨画布 */
.stars-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

/* 粒子画布 */
.particles-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

/* 科技感网格背景 */
#homePage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(255, 184, 108, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 184, 108, 0.05) 1px, transparent 1px),
    linear-gradient(rgba(255, 111, 97, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 111, 97, 0.04) 1px, transparent 1px);
  background-size:
    100px 100px,
    100px 100px,
    20px 20px,
    20px 20px;
  pointer-events: none;
  animation: gridFloat 20s ease-in-out infinite;
}

/* 动态光效 */
#homePage::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(
      600px circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(255, 184, 108, 0.1) 0%,
      rgba(255, 111, 97, 0.08) 40%,
      transparent 80%
    ),
    linear-gradient(45deg, transparent 30%, rgba(255, 184, 108, 0.05) 50%, transparent 70%),
    linear-gradient(-45deg, transparent 30%, rgba(255, 111, 97, 0.05) 50%, transparent 70%);
  pointer-events: none;
  animation: lightPulse 8s ease-in-out infinite alternate;
}

@keyframes gridFloat {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(5px, 5px);
  }
}

@keyframes lightPulse {
  0% {
    opacity: 0.3;
  }
  100% {
    opacity: 0.7;
  }
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 20;
  width: 100%;
  box-sizing: border-box;
}

/* 移除居中光束效果 */

/* 英雄区域 */
.hero-section {
  text-align: center;
  padding: 56px 0 36px;
  margin-bottom: 20px;
  color: #1e293b;
  position: relative;
  overflow: hidden;
}

.hero-logo {
  height: 84px;
  width: 84px;
  display: inline-block;
  border-radius: 18px;
  box-shadow: 0 10px 28px rgba(255, 138, 91, 0.22);
  margin-bottom: 16px;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse 800px 400px at center, rgba(255, 184, 108, 0.16) 0%, transparent 70%),
    linear-gradient(45deg, transparent 30%, rgba(255, 111, 97, 0.06) 50%, transparent 70%),
    linear-gradient(-45deg, transparent 30%, rgba(255, 138, 91, 0.05) 50%, transparent 70%);
  animation: heroGlow 10s ease-in-out infinite alternate;
}

@keyframes heroGlow {
  0% {
    opacity: 0.6;
    transform: scale(1);
  }
  100% {
    opacity: 1;
    transform: scale(1.02);
  }
}

@keyframes rotate {
  0% {
    transform: translate(-50%, -50%) rotate(0deg);
  }
  100% {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

.hero-title {
  font-size: 56px;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.2;
  background: linear-gradient(135deg, #ffb86c 0%, #ff8a5b 50%, #ff6f61 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1px;
  position: relative;
  z-index: 2;
  animation: titleShimmer 3s ease-in-out infinite;
}

@keyframes titleShimmer {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.hero-description {
  font-size: 20px;
  margin: 0;
  opacity: 0.8;
  color: #64748b;
  position: relative;
  z-index: 2;
}

/* 输入区域 */
.input-section {
  position: relative;
  margin: 0 auto 20px;
  max-width: 800px;
}

.prompt-input {
  border-radius: 16px;
  border: none;
  font-size: 16px;
  padding: 20px 60px 20px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.prompt-input:focus {
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.3);
  transform: translateY(-2px);
}

.input-actions {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.create-btn {
  width: 48px;
  height: 48px;
  min-width: 48px;
  padding: 0 !important;
  border-radius: 50% !important;
  display: inline-grid;
  place-items: center;
  line-height: 48px;
  transform: scale(0.8);
  background: linear-gradient(135deg, #ffb86c, #ff8a5b 60%, #ff6f61);
  border: none;
  box-shadow:
    0 10px 22px rgba(255, 138, 91, 0.28),
    0 4px 10px rgba(255, 111, 97, 0.16);
}
.create-btn:hover {
  background: linear-gradient(135deg, #ffc488, #ff996f 60%, #ff7c70);
  transform: translateY(-2px);
  box-shadow:
    0 14px 28px rgba(255, 138, 91, 0.34),
    0 6px 14px rgba(255, 111, 97, 0.2);
}
.create-btn:active {
  transform: translateY(0);
  box-shadow:
    0 8px 18px rgba(255, 138, 91, 0.28),
    0 3px 10px rgba(255, 111, 97, 0.18);
}
.create-btn :deep(.ant-btn-icon) {
  line-height: 1;
}
.create-btn span {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

/* 快捷卡片 */
.quick-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  max-width: 800px;
  margin: 0 auto 80px;
}

.quick-card {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 184, 108, 0.25);
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.06),
    0 1px 4px rgba(255, 184, 108, 0.08);
}

.quick-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 184, 108, 0.2), transparent);
  transition: left 0.5s ease;
}

.quick-card:hover::before {
  left: 100%;
}

.quick-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 184, 108, 0.5);
  box-shadow:
    0 6px 16px rgba(255, 184, 108, 0.2),
    0 3px 8px rgba(255, 111, 97, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.quick-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.5;
  white-space: nowrap;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 184, 108, 0.2);
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.06),
    0 2px 6px rgba(255, 184, 108, 0.08);
}

.quick-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 184, 108, 0.2), transparent);
  transition: left 0.6s ease;
}

.quick-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(
    circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
    rgba(255, 184, 108, 0.15),
    transparent 60%
  );
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.quick-card:hover::before {
  left: 100%;
}

.quick-card:hover::after {
  opacity: 1;
}

.quick-card:hover {
  transform: translateY(-3px) scale(1.02);
  border-color: rgba(255, 184, 108, 0.5);
  box-shadow:
    0 8px 24px rgba(255, 184, 108, 0.25),
    0 4px 12px rgba(255, 111, 97, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.quick-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

.quick-card:hover .quick-icon {
  transform: scale(1.2) rotate(-5deg);
}

.quick-content {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.quick-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
  line-height: 1.3;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.quick-desc {
  font-size: 11px;
  color: #64748b;
  line-height: 1.3;
  opacity: 0.85;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 作品卡片 */
.works-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px;
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.08),
    0 4px 16px rgba(255, 184, 108, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.9);
  margin-bottom: 40px;
}

.works-tabs {
  margin: 0 !important;
}

.works-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 32px !important;
}

.works-tabs :deep(.ant-tabs-tab) {
  font-size: 18px;
  font-weight: 600;
  color: #64748b;
  padding: 12px 32px !important;
  transition: all 0.3s ease;
}

.works-tabs :deep(.ant-tabs-tab:hover) {
  color: #ff8a5b;
}

.works-tabs :deep(.ant-tabs-tab-active) {
  color: #ff8a5b;
  font-size: 19px;
}

.works-tabs :deep(.ant-tabs-ink-bar) {
  background: linear-gradient(90deg, #ffb86c, #ff8a5b, #ff6f61);
  height: 3px;
  border-radius: 3px;
}

.tab-content {
  min-height: 300px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.8;
}

.empty-text {
  font-size: 16px;
  color: #64748b;
}

/* 区域标题 */
.section {
  margin-bottom: 60px;
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 32px;
  color: #1e293b;
}

/* 我的作品网格 */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 精选案例网格 */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.pagination-wrapper :deep(.ant-pagination-item) {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.pagination-wrapper :deep(.ant-pagination-item-active) {
  background: linear-gradient(135deg, #ffb86c, #ff8a5b);
  border-color: transparent;
}

.pagination-wrapper :deep(.ant-pagination-item-active a) {
  color: white;
}

.pagination-wrapper :deep(.ant-pagination-item:hover) {
  border-color: #ffb86c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-description {
    font-size: 16px;
  }

  .app-grid,
  .featured-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    gap: 12px;
  }

  .quick-card {
    padding: 5px 14px;
  }

  .quick-title {
    font-size: 13px;
  }

  .works-card {
    padding: 20px;
    border-radius: 20px;
  }

  .works-tabs :deep(.ant-tabs-tab) {
    font-size: 16px;
    padding: 10px 20px !important;
  }

  .works-tabs :deep(.ant-tabs-tab-active) {
    font-size: 17px;
  }
}

@media (max-width: 480px) {
  .quick-actions {
    gap: 10px;
  }

  .quick-card {
    padding: 5px 12px;
  }

  .quick-title {
    font-size: 12px;
  }
}
</style>
