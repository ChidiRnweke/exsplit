<script lang="ts">
	import Heading from 'flowbite-svelte/Heading.svelte';
	export let title = '';
	export let alignment: 'left' | 'center' | 'right' = 'center';
	export let marginBottom: '0' | '5' | '10' | '20' | '30' = '20';
	let classText = `text-primary dark:text-dark-primary mb-${marginBottom} text-${alignment} font-extrabold`;
</script>

<Heading tag="h2" class={classText}>
	{title}
</Heading>
