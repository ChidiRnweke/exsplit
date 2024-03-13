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
	class=" bg-primary dark:bg-dark-primary hover:bg-accent dark:hover:bg-dark-accent self-stretch text-lg font-extrabold"
>
	{#if arrowDirection == 'left'}
		<svelte:component this={arrow} />
	{/if}
	<slot />
	{#if arrowDirection == 'right'}
		<svelte:component this={arrow} />
	{/if}
</Button>
