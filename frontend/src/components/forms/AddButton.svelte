<script lang="ts">
	import Plus from '~icons/mdi/plus';
	export let action: (ev: Event) => void = () => {};
</script>

<button on:click|preventDefault={action}>
	<Plus
		class="relative h-auto w-10 place-self-center rounded-full bg-accent text-white hover:bg-primary dark:bg-accent dark:hover:bg-primary lg:place-self-start"
	/>
</button>
