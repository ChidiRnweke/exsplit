<script lang="ts">
	import Button from 'flowbite-svelte/Button.svelte';
	import ArrowRight from '~icons/mdi/arrow-right';
	import ArrowLeft from '~icons/mdi/arrow-left';
	export let formAction: (ev: Event) => void = () => {};
	export let disabled = false;
	export let arrowDirection: 'left' | 'right' = 'right';
	$: arrow = arrowDirection == 'right' ? ArrowRight : ArrowLeft;
</script>

<Button
	{disabled}
	type="submit"
	on:click={formAction}
	class=" self-stretch bg-primary text-lg font-extrabold hover:bg-accent dark:bg-dark-primary dark:hover:bg-dark-accent"
>
	{#if arrowDirection == 'left'}
		<svelte:component this={arrow} />
	{/if}
	<slot />
	{#if arrowDirection == 'right'}
		<svelte:component this={arrow} />
	{/if}
</Button>
