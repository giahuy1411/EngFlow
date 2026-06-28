<template>
  <div>
    <div v-for="section in sections" :key="section.id" class="mb-8">
      <h2 class="text-xl font-bold mb-4">{{ section.title }}</h2>
      <div v-for="block in section.blocks" :key="block.id" class="mb-4">
        <TextBlock v-if="block.blockType === 'TEXT'" :data="parseData(block.data)" />
        <ImageBlock v-else-if="block.blockType === 'IMAGE'" :data="parseData(block.data)" />
        <AudioBlock v-else-if="block.blockType === 'AUDIO'" :data="parseData(block.data)" />
        <TableBlock v-else-if="block.blockType === 'TABLE'" :data="parseData(block.data)" />
        <QuestionBlock v-else-if="block.blockType === 'QUESTION'" :data="parseData(block.data)" :block-id="block.id" />
        <SubmissionBlock v-else-if="block.blockType === 'SUBMISSION'" :data="parseData(block.data)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import TextBlock from './TextBlock.vue';
import ImageBlock from './ImageBlock.vue';
import AudioBlock from './AudioBlock.vue';
import TableBlock from './TableBlock.vue';
import QuestionBlock from './QuestionBlock.vue';
import SubmissionBlock from './SubmissionBlock.vue';

const props = defineProps({
  sections: { type: Array, required: true }
});

function parseData(data) {
  if (!data) return {};
  if (typeof data === 'string') {
    try { return JSON.parse(data); } catch { return { content: data }; }
  }
  return data;
}
</script>
