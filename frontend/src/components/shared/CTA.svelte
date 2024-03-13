<script>
	import Button from 'flowbite-svelte/Button.svelte';
	import ArrowRight from '~icons/mdi/arrow-right';
	export let href = '';
</script>

<Button
	{href}
	class="bg-primary dark:bg-dark-primary hover:bg-accent dark:hover:bg-dark-accent mt-8"
>
	Get Started <ArrowRight />
</Button>
