<!--
  * 顶部菜单-递归菜单
  * 
  * @Author:    1024创新实验室-主任：卓大 
  * @Date:      2022-09-06 20:29:12 
  * @Wechat:    zhuda1024 
  * @Email:     lab1024@163.com 
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012 
-->
<template>
  <a-menu
    v-model:openKeys="openKeys"
    v-model:selectedKeys="selectedKeys"
    class="smart-menu"
    mode="horizontal"
    :theme="theme"
  >
    <template v-for="item in menuTree" :key="item.menuId">
      <template v-if="item.visibleFlag && !item.disabledFlag">
        <template v-if="$lodash.isEmpty(item.children)">
          <a-menu-item :key="item.menuId" @click="turnToPage(item)">
            <template #icon>
              <component :is="$antIcons[item.icon]" />
            </template>
            {{ item.menuName }}
          </a-menu-item>
        </template>
        <template v-else>
          <SubMenu :menu-info="item" :key="item.menuId" @turnToPage="turnToPage" />
        </template>
      </template>
    </template>
  </a-menu>
</template>
<script setup lang="ts">
  import _ from 'lodash';
  import { computed, ref, watch, nextTick } from 'vue';
  import { useRoute } from 'vue-router';
  import SubMenu from './sub-menu.vue';
  import { router } from '/@/router/index';
  import { useAppConfigStore } from '/@/store/modules/system/app-config';
  import { useUserStore } from '/@/store/modules/system/user';

  const theme = computed(() => useAppConfigStore().$state.sideMenuTheme);

  const menuTree = computed(() => useUserStore().getMenuTree || []);

  //展开的菜单
  let currentRoute = useRoute();
  const selectedKeys = ref([]);
  const openKeys = ref([]);

  // 页面跳转
  function turnToPage(menu) {
    if (!menu || !menu.path) {
      console.warn('菜单参数不完整:', menu);
      return;
    }

    try {
      // 安全地删除缓存
      const userStore = useUserStore();
      if (userStore && typeof userStore.deleteKeepAliveIncludes === 'function') {
        userStore.deleteKeepAliveIncludes(menu.menuId.toString());
      }

      // 使用nextTick确保OM更新完成后再跳转
      nextTick(() => {
        router.push({ path: menu.path }).catch(err => {
          // 忽略重复导航错误
          if (err.name !== 'NavigationDuplicated') {
            console.error('路由跳转失败:', err);
          }
        });
      });
    } catch (error) {
      console.error('页面跳转失败:', error);
    }
  }

  /**
   * SmartAdmin中 router的name 就是 后端存储menu的id
   * 所以此处可以直接监听路由，根据路由更新菜单的选中和展开
   */
  function updateSelectKeys() {
    // 更新选中
    selectedKeys.value = [_.toNumber(currentRoute.name)];
  }

  watch(
    currentRoute,
    () => {
      updateSelectKeys();
    },
    {
      immediate: true,
    }
  );

  defineExpose({
    updateSelectKeys,
  });
</script>

<style lang="less" scoped>
  .smart-menu {
    position: relative;
  }

</style>
