import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type TransactionDocument = HydratedDocument<Transaction>;

@Schema({ ...stellaSchemaOptions, collection: 'Transaction' })
export class Transaction {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true, enum: ['ingress', 'egress'] })
  type!: 'ingress' | 'egress';

  @Prop({ required: true, type: Number })
  amount!: number;

  @Prop({ required: true })
  category!: string;

  @Prop({ type: String, default: null })
  description!: string | null;

  @Prop({ type: Date, required: true })
  date!: Date;

  @Prop({ type: String, default: null })
  linkedTaskId!: string | null;

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date, default: null })
  deletedAt!: Date | null;
}

export const TransactionSchema = SchemaFactory.createForClass(Transaction);
TransactionSchema.index({ updatedAt: 1 });
TransactionSchema.index({ date: 1, type: 1 });
