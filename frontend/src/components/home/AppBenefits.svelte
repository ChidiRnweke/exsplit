<script lang="ts">
	import BenefitSection from './BenefitSection.svelte';
	import CalenderIcon from '~icons/mdi/calendar-blank-outline';
	import CircleArrows from '~icons/mdi/circle-arrows';
	import MdiConnection from '~icons/mdi/connection';
	import BellNotification from '~icons/mdi/bell-notification-outline';
	const benefitCards = [
		{
			title: 'Smart Priority Scheduling',
			paragraph:
				'AIsapp uses advanced algorithms to rank your tasks based on priority and urgency, ensuring you focus on what matters most.',
			IconComponent: CalenderIcon,
			tagLine: 'Plan smart.'
		},
		{
			title: 'Adaptive Time Management',
			paragraph:
				'AIsapp app learns from your habits and adjusts your schedule for optimal efficiency.',
			IconComponent: CircleArrows,
			tagLine: 'AI powered.'
		},
		{
			title: 'Seamless integration',
			paragraph: ` 
			If you prefer using your existing apps AIsapp has you covered. 
			You can use it as a plug-and-play scheduler and remain in your comfort zone.`,
			IconComponent: MdiConnection,
			tagLine: 'Integrate with ease.'
		},
		{
			title: 'Customizable Notifications',
			paragraph: 'Set personalized reminders for tasks and appointments, so you never miss a beat.',
			IconComponent: BellNotification,
			tagLine: 'Be in control.'
		}
	];
</script>

{#each benefitCards as benefit}
	<BenefitSection {...benefit}></BenefitSection>
{/each}
