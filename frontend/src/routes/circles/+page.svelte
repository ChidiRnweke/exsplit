<script>
	import HeaderSection from '../../components/shared/HeaderSection.svelte';
	import Card from 'flowbite-svelte/Card.svelte';
	import ImagePlaceholder from 'flowbite-svelte/ImagePlaceholder.svelte';
	let hCard = false;
</script>

<HeaderSection class="mb-20">Your circles</HeaderSection>

<div class="grid gap-4 md:grid-cols-4">
	<Card href="/" horizontal size="lg" reverse={hCard}>
		<section class="grid grid-cols-2 gap-4">
			<ImagePlaceholder />
			<div>
				<h2 class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
					Noteworthy technology acquisitions 2021
				</h2>
				<p class="mb-3 font-normal leading-tight text-gray-700 dark:text-gray-400">
					Here are the biggest enterprise technology acquisitions of 2021 so far, in reverse
					chronological order.
				</p>
			</div>
		</section>
	</Card>

	<Card href="/" horizontal size="lg" reverse={hCard}>
		<section class="grid grid-cols-2 gap-4">
			<ImagePlaceholder />
			<div>
				<h2 class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
					Noteworthy technology acquisitions 2021
				</h2>
				<p class="mb-3 font-normal leading-tight text-gray-700 dark:text-gray-400">
					Here are the biggest enterprise technology acquisitions of 2021 so far, in reverse
					chronological order.
				</p>
			</div>
		</section>
	</Card>
</div>
