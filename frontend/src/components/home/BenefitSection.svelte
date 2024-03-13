<script lang="ts">
	import P from 'flowbite-svelte/P.svelte';
	import Heading from 'flowbite-svelte/Heading.svelte';
	import ImagePlaceholder from 'flowbite-svelte/ImagePlaceholder.svelte';
	export let title: string;
	export let paragraph: string;
	import type { SvelteComponent } from 'svelte';
	export let IconComponent: new (...args: any) => SvelteComponent;
	export let image: typeof ImagePlaceholder = ImagePlaceholder;
	export let tagLine: string;
</script>

<section class="mt-12">
	<div class="md:grid md:grid-cols-2 gap-10">
		<div class="grid grid-cols-1">
			<Heading tag="h3" class="text-secondary dark:text-dark-secondary font-extrabold"
				>{title}
			</Heading>

			<P class="text-accent dark:text-dark-accent text-lg mb-6 ">{tagLine}</P>
			<svelte:component
				this={IconComponent}
				class="w-20 h-auto text-accent dark:text-dark-accent relative bg-background-alt dark:bg-dark-background-alt rounded-lg p-6 mb-3 place-self-center  lg:place-self-start"
			/>
			<P class="text-foreground dark:text-dark-foreground ">{paragraph}</P>
		</div>
		<svelte:component this={image} />
	</div>
</section>
