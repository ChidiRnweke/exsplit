<script>
	import Button from 'flowbite-svelte/Button.svelte';
	import ArrowRight from '~icons/mdi/arrow-right';
	export let href = '';
</script>

<Button
	{href}
	class="mt-8 bg-primary hover:bg-accent dark:bg-dark-primary dark:hover:bg-dark-accent"
>
	Get Started <ArrowRight />
</Button>
